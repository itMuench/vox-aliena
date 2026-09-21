#pragma once

#include <voicesmith/Header.h>

#include <StftPitchShift/FFT.h>
#include <StftPitchShift/Normalizer.h>
#include <StftPitchShift/Vocoder.h>
#include <StftPitchShift/Pitcher.h>
#include <StftPitchShift/Cepster.h>
#include <StftPitchShift/Resampler.h>

/**
 * StftPitchShiftCore variant used by Voicesmith/Vox Aliena.
 *
 * Upstream's Resampler intentionally zero-fills the part of a vector that
 * falls outside the resampled range when factor < 1. That is correct for
 * pitch resampling, but it can make a downward-shifted spectral envelope
 * collapse to (near) zero and mute real-time voice frames.
 *
 * Keep a small amount of the original spectral envelope as a floor only for
 * downward timbre shifts. The shifted envelope still dominates, while the
 * residual can no longer be multiplied by an all-zero/near-zero envelope.
 */
template<class T>
class TimbreSafeStftPitchShiftCore
{

public:

  TimbreSafeStftPitchShiftCore(
    const std::shared_ptr<stftpitchshift::FFT> fft,
    const std::tuple<size_t, size_t> framesize,
    const size_t hopsize,
    const double samplerate) :
    vocoder(framesize, hopsize, samplerate),
    pitcher(std::get<0>(framesize), samplerate),
    cepster(fft, std::get<0>(framesize), samplerate),
    envelope(std::get<0>(framesize) / 2 + 1),
    originalEnvelope(std::get<0>(framesize) / 2 + 1)
  {
  }

  const std::vector<double>& factors() const
  {
    return pitcher.factors();
  }

  void factors(const std::vector<double>& factors)
  {
    pitcher.factors(factors);
    vocoder.reset();
  }

  double quefrency() const
  {
    return cepster.quefrency();
  }

  void quefrency(const double quefrency)
  {
    cepster.quefrency(quefrency);
  }

  double distortion() const
  {
    return resampler.factor();
  }

  void distortion(const double distortion)
  {
    resampler.factor(distortion);
  }

  bool normalization() const
  {
    return static_cast<bool>(normalizer);
  }

  void normalization(const bool normalization)
  {
    if (normalization)
    {
      normalizer = std::make_shared<Normalizer<T>>();
    }
    else
    {
      normalizer = nullptr;
    }
  }

  void shiftpitch(const std::span<std::complex<T>> dft)
  {
    vocoder.encode(dft);

    if (normalizer)
    {
      normalizer->calibrate(dft);
    }

    if (cepster.quefrency())
    {
      for (size_t i = 0; i < dft.size(); ++i)
      {
        envelope[i] = dft[i].real();
      }

      cepster.lifter(envelope);

      for (size_t i = 0; i < dft.size(); ++i)
      {
        if (std::isnormal(envelope[i]))
        {
          dft[i].real(dft[i].real() / envelope[i]);
        }
        else
        {
          envelope[i] = 0;
          dft[i].real(0);
        }
      }

      std::copy(envelope.begin(), envelope.end(), originalEnvelope.begin());

      resampler.linear(envelope);
      stabilizeDownwardTimbreShift();

      pitcher.shiftpitch(dft);

      for (size_t i = 0; i < dft.size(); ++i)
      {
        dft[i].real(dft[i].real() * envelope[i]);
      }
    }
    else
    {
      pitcher.shiftpitch(dft);
    }

    if (normalizer)
    {
      normalizer->normalize(dft);
    }

    vocoder.decode(dft);
  }

private:

  // -26 dB relative to the original envelope: low enough to preserve the
  // timbre shift, high enough to prevent a collapsed envelope from muting.
  static constexpr T minimumEnvelopeRatio = T(0.05);

  stftpitchshift::Vocoder<T> vocoder;
  stftpitchshift::Pitcher<T> pitcher;
  stftpitchshift::Cepster<T> cepster;
  stftpitchshift::Resampler<T> resampler;

  std::vector<T> envelope;
  std::vector<T> originalEnvelope;

  std::shared_ptr<Normalizer<T>> normalizer;

  void stabilizeDownwardTimbreShift()
  {
    if (resampler.factor() >= 1)
    {
      return;
    }

    for (size_t i = 0; i < envelope.size(); ++i)
    {
      const T floor = originalEnvelope[i] * minimumEnvelopeRatio;

      if (!std::isfinite(envelope[i]) || envelope[i] < floor)
      {
        envelope[i] = floor;
      }
    }
  }

};
