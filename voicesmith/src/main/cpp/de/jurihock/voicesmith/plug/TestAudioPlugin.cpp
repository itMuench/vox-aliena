#include <voicesmith/plug/TestAudioPlugin.h>

#include <voicesmith/Source.h>

#include <voicesmith/io/AudioSink.h>
#include <voicesmith/io/AudioSource.h>

#include <voicesmith/fx/BypassEffect.h>
#include <voicesmith/fx/NoiseEffect.h>
#include <voicesmith/fx/NullEffect.h>
#include <voicesmith/fx/SineEffect.h>
#include <voicesmith/fx/SweepEffect.h>
#include <voicesmith/fx/VadEffect.h>

TestAudioPlugin::TestAudioPlugin(jna_callback* callback) :
  callback(callback) {
  state.liveEffects = std::make_shared<StereoChainEffect<DelayEffect, PitchTimbreShiftEffect>>();
  state.recordingEffects = std::make_shared<StereoChainEffect<PitchTimbreShiftEffect>>();
}

TestAudioPlugin::~TestAudioPlugin() {
  try {
    stop();
  } catch (const std::exception& exception) {
    Log::e("Unable to stop audio plugin: {0}", exception.what());
  }
}

void TestAudioPlugin::setup(const std::optional<int> input,
                            const std::optional<int> output,
                            const std::optional<float> samplerate,
                            const std::optional<size_t> blocksize,
                            const std::optional<size_t> channels) {
  config.input = input;
  config.output = output;
  config.samplerate = samplerate;
  config.blocksize = blocksize;
  config.channels = channels;
}

void TestAudioPlugin::set(const std::string& param,
                          const std::string& value) {
  if (param == "delay") {
    state.liveEffects->get<DelayEffect>([value](auto effect){
      effect->delay(value);
    });
  }

  if (param == "pitch") {
    state.liveEffects->get<PitchTimbreShiftEffect>([value](auto effect){
      effect->pitch(value);
    });
    state.recordingEffects->get<PitchTimbreShiftEffect>([value](auto effect){
      effect->pitch(value);
    });
  }

  if (param == "timbre") {
    state.liveEffects->get<PitchTimbreShiftEffect>([value](auto effect){
      effect->timbre(value);
    });
    state.recordingEffects->get<PitchTimbreShiftEffect>([value](auto effect){
      effect->timbre(value);
    });
  }
}

void TestAudioPlugin::start() {
  if (state.pipeline != nullptr || state.recordingSource != nullptr) {
    return;
  }

  auto source = std::make_shared<AudioSource>(config.input, config.samplerate, config.blocksize, config.channels);
  auto sink = std::make_shared<AudioSink>(config.output, config.samplerate, config.blocksize, config.channels);
  auto pipe = std::make_shared<AudioPipeline>(source, sink, state.liveEffects);

  pipe->subscribe([&](const AudioEventCode code, const std::string& data){
    callback(!code, data.c_str());
  });

  state.pipeline = pipe;
  state.pipeline->open();
  state.pipeline->start();
}

void TestAudioPlugin::startRecording(const std::string& path) {
  if (state.pipeline != nullptr || state.recordingSource != nullptr) {
    return;
  }

  if (path.empty()) {
    throw std::runtime_error("Invalid MP3 output path!");
  }

  auto source = std::make_shared<AudioSource>(
    config.input,
    config.samplerate,
    config.blocksize,
    config.channels);

  source->subscribe([&](const AudioEventCode code, const std::string& data){
    callback(!code, data.c_str());
  });

  source->open();

  const auto samplerate = source->samplerate();
  const auto blocksize = source->blocksize();
  const auto channels = source->channels();

  const auto framesPerBlock = std::max<size_t>(1, blocksize / channels);
  const auto fifosize = static_cast<size_t>(std::ceil(
    samplerate / static_cast<double>(framesPerBlock)));

  source->fifo()->resize(std::max<size_t>(2, fifosize), blocksize);

  auto recorder = std::make_shared<Mp3Recorder>(
    source->fifo(),
    state.recordingEffects,
    path,
    samplerate,
    blocksize,
    channels);

  try {
    recorder->start();
    source->start();
  } catch (...) {
    try {
      recorder->stop();
    } catch (...) {
    }
    source->close();
    throw;
  }

  state.recordingSource = source;
  state.recorder = recorder;
}

void TestAudioPlugin::stop() {
  if (state.pipeline != nullptr) {
    state.pipeline->stop();
    state.pipeline->close();
    state.pipeline = nullptr;
  }

  if (state.recordingSource != nullptr) {
    std::exception_ptr failure;

    try {
      state.recordingSource->stop();
      if (state.recorder != nullptr) {
        state.recorder->stop();
      }
    } catch (...) {
      failure = std::current_exception();
    }

    state.recordingSource->close();
    state.recordingSource->fifo()->flush();
    state.recorder = nullptr;
    state.recordingSource = nullptr;

    if (failure) {
      std::rethrow_exception(failure);
    }
  }
}
