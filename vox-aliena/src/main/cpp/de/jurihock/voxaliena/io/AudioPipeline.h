#pragma once

#include <voxaliena/Header.h>

#include <voxaliena/etc/Debouncer.h>
#include <voxaliena/etc/Timer.h>
#include <voxaliena/fx/AudioEffect.h>
#include <voxaliena/io/AudioEvent.h>
#include <voxaliena/io/AudioSink.h>
#include <voxaliena/io/AudioSource.h>

class AudioPipeline final : public AudioEvent::Emitter {

public:

  AudioPipeline(const std::shared_ptr<AudioSource> source,
                const std::shared_ptr<AudioSink> sink,
                const std::shared_ptr<AudioEffect> effect = nullptr);

  ~AudioPipeline();

  void subscribe(const AudioEvent::Callback& callback) override;

  void open();
  void close();

  void start();
  void stop();

private:

  struct timers_t {
    Timer<std::chrono::milliseconds> outer;
    Timer<std::chrono::milliseconds> inner;
  };

  struct debouncers_t {
    Debouncer read;
    Debouncer write;
  };

  const std::shared_ptr<AudioSource> source;
  const std::shared_ptr<AudioSink> sink;
  const std::shared_ptr<AudioEffect> effect;

  struct {

    std::shared_ptr<std::thread> thread;
    std::condition_variable signal;
    std::mutex mutex;
    bool loop;

  } state;

  AudioEvent event;
  std::mutex eventmutex;

  void onloop();
  bool oncycle(timers_t& timers, debouncers_t& debouncers, uint64_t& index, const std::chrono::milliseconds& timeout) const;
  void onevent(const AudioEventCode code, const std::string& data);

};
