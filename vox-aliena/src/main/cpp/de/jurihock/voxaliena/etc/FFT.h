#pragma once

#include <voxaliena/Header.h>

#include <voxaliena/etc/FFT/PocketFFT.h>
#include <voxaliena/etc/FFT/PrettyFastFFT.h>

using FFT = std::conditional_t<std::is_same_v<fft_t, double>, PocketFFT, PrettyFastFFT>;
