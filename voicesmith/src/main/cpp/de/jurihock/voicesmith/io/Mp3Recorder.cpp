#include <voicesmith/io/Mp3Recorder.h>

#include <voicesmith/Source.h>

Mp3Recorder::Mp3Recorder(const std::shared_ptr<AudioBlockQueue> queue,
                         const std::shared_ptr<AudioEffect> effect,
                         const std::string& path,
                         const float samplerate,
                         const size_t blocksize,
                         const size_t channels) :
  queue(queue),
  effect(effect),
  path(path),
  samplerate(static_cast<int>(std::lround(samplerate))),
  blocksize(blocksize),
  channels(channels) {
  if (channels < 1 || channels > 2) {
    throw std::runtime_error("MP3 recording supports mono or stereo only!");
  }
}

Mp3Recorder::~Mp3Recorder() {
  try {
    stop();
  } catch (const std::exception& exception) {
    Log::e("Unable to stop MP3 recorder: {0}", exception.what());
  }
}

void Mp3Recorder::start() {
  if (thread != nullptr) {
    return;
  }

  failure = nullptr;
  initialise();

  try {
    running = true;
    thread = std::make_unique<std::thread>([&]() { loop(); });
  } catch (...) {
    running = false;
    cleanup();
    throw;
  }
}

void Mp3Recorder::stop() {
  running = false;

  if (thread != nullptr) {
    if (thread->joinable()) {
      thread->join();
    }
    thread = nullptr;
  } else {
    cleanup();
  }

  if (failure) {
    const auto error = failure;
    failure = nullptr;
    std::rethrow_exception(error);
  }
}

void Mp3Recorder::initialise() {
  shine_config_t config {};

  config.wave.channels = (channels == 1) ? PCM_MONO : PCM_STEREO;
  config.wave.samplerate = samplerate;

  shine_set_config_mpeg_defaults(&config.mpeg);
  config.mpeg.bitr = bitrate;
  config.mpeg.mode = (channels == 1) ? MONO : JOINT_STEREO;

  if (shine_check_config(config.wave.samplerate, config.mpeg.bitr) < 0) {
    throw std::runtime_error(
      $("Unsupported MP3 sample rate {0} Hz at {1} kbps!",
        config.wave.samplerate, config.mpeg.bitr));
  }

  encoder = shine_initialise(&config);
  if (encoder == nullptr) {
    throw std::runtime_error("Unable to initialize MP3 encoder!");
  }

  const auto samplesPerPass = shine_samples_per_pass(encoder);
  if (samplesPerPass <= 0) {
    cleanup();
    throw std::runtime_error("Invalid MP3 encoder frame size!");
  }

  samplesPerFrame = static_cast<size_t>(samplesPerPass) * channels;
  pcm.clear();
  pcm.reserve(samplesPerFrame);

  output.open(path, std::ios::binary | std::ios::trunc);
  if (!output.is_open()) {
    cleanup();
    throw std::runtime_error("Unable to create MP3 output file!");
  }

  if (effect) {
    effect->reset(
      static_cast<float>(samplerate),
      blocksize,
      channels);
  }
}

void Mp3Recorder::loop() {
  try {
    const auto timeout = std::chrono::milliseconds(20);
    auto index = uint64_t(0);
    AudioBlock processed(blocksize);

    while (running || !queue->empty()) {
      queue->read(timeout, [&](AudioBlock& input) {
        if (effect) {
          effect->apply(index, input, processed);
        } else {
          input.copyto(processed);
        }

        append(processed);
        ++index;
      });
    }

    finish();
  } catch (...) {
    failure = std::current_exception();
  }

  cleanup();
}

void Mp3Recorder::append(const AudioBlock& block) {
  const std::span<const float> samples = block;

  for (const auto sample : samples) {
    const auto value = std::clamp(sample, -1.f, 1.f);
    const auto scaled = std::lround(value * 32767.f);
    pcm.push_back(static_cast<int16_t>(scaled));

    if (pcm.size() == samplesPerFrame) {
      encode();
    }
  }
}

void Mp3Recorder::encode() {
  if (pcm.size() != samplesPerFrame) {
    return;
  }

  int written = 0;
  const auto data = shine_encode_buffer_interleaved(
    encoder, pcm.data(), &written);

  write(data, written);
  pcm.clear();
}

void Mp3Recorder::write(const unsigned char* data, const int size) {
  if (data == nullptr || size <= 0) {
    return;
  }

  output.write(
    reinterpret_cast<const char*>(data),
    static_cast<std::streamsize>(size));

  if (!output.good()) {
    throw std::runtime_error("Unable to write MP3 output file!");
  }
}

void Mp3Recorder::finish() {
  if (!pcm.empty()) {
    pcm.resize(samplesPerFrame, 0);
    encode();
  }

  int written = 0;
  const auto data = shine_flush(encoder, &written);
  write(data, written);

  output.flush();
  if (!output.good()) {
    throw std::runtime_error("Unable to finalize MP3 output file!");
  }
}

void Mp3Recorder::cleanup() {
  running = false;

  if (encoder != nullptr) {
    shine_close(encoder);
    encoder = nullptr;
  }

  if (output.is_open()) {
    output.close();
  }

  pcm.clear();
}
