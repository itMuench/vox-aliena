#pragma once

#include <voxaliena/Header.h>

#include <voxaliena/io/AudioEventCode.h>
#include <voxaliena/io/AudioEventCodeExtensions.h>

#include <eventpp/callbacklist.h>

class AudioEvent final : public eventpp::CallbackList<void(const AudioEventCode code, const std::string& data)> {

public:

  class Emitter {

  public:

    virtual ~Emitter() = default;

    virtual void subscribe(const Callback& callback) = 0;

  };

};
