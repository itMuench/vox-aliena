#pragma once

#include <voxaliena/Header.h>

#include <voxaliena/io/AudioEventCode.h>

constexpr int operator!(AudioEventCode code) noexcept {
  return static_cast<int>(code);
}
