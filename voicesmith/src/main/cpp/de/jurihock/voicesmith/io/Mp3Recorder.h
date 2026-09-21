#pragma once

#include <voicesmith/Header.h>

#include <voicesmith/fx/AudioEffect.h>
#include <voicesmith/io/AudioBlockQueue.h>

extern "C" {
#include <layer3.h>
}

#include <atomic>
#include <exception>
#include <fstream>

class Mp3Recorder final {

public:

  Mp3Recorder(const std::shared_ptr<AudioBlockQueue> queue,
              const std::shared_ptr<AudioEffect> effect,
              const std::string& path,
              const float samplerate,
              const size_t blocksize,
              const size_t channels);

  ~Mp3Recorder();

  void start();
  void stop();

private:

  static constexpr int bitrate = 128;

  const std::shared_ptr<AudioBlockQueue> queue;
  const std::shared_ptr<AudioEffect> effect;
  const std::string path;
  const int samplerate;
  const size_t blocksize;
  const size_t channels;

  std::atomic_bool running = false;
  std::unique_ptr<std::thread> thread;
  std::exception_ptr failure;

  std::ofstream output;
  shine_t encoder = nullptr;
  size_t samplesPerFrame = 0;
  std::vector<int16_t> pcm;

  void initialise();
  void loop();
  void append(const AudioBlock& block);
  void encode();
  void write(const unsigned char* data, const int size);
  void finish();
  void cleanup();

};
