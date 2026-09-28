#pragma once

#include <voxaliena/Header.h>

#include <voxaliena/etc/Debouncer.h>
#include <voxaliena/fx/AudioEffect.h>
#include <voxaliena/io/AudioBlockQueue.h>
#include <voxaliena/io/AudioStream.h>

class AudioSink final : public AudioStream {

public:

  AudioSink(const std::optional<int> device = std::nullopt,
            const std::optional<float> samplerate = std::nullopt,
            const std::optional<size_t> blocksize = std::nullopt,
            const std::optional<size_t> channels = std::nullopt,
            const std::shared_ptr<AudioEffect> effect = nullptr,
            const std::shared_ptr<AudioBlockQueue> queue = nullptr);

  std::shared_ptr<AudioEffect> fx() const;
  std::shared_ptr<AudioBlockQueue> fifo() const;

protected:

  void callback(const std::span<float> samples) override;

  void onopen() override;
  void onstart() override;

private:

  const std::shared_ptr<AudioEffect> effect;
  const std::shared_ptr<AudioBlockQueue> queue;

  struct {

    struct {
      uint64_t inner;
      uint64_t outer;
    } index;

    Debouncer underflow;

  } state;

};
