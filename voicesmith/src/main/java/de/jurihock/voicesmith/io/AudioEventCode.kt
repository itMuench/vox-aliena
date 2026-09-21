package de.jurihock.voicesmith.io

enum class AudioEventCode {
  INFO,
  WARNING,
  SourceOverflow,
  SourceOverrun,
  SinkUnderflow,
  SinkUnderrun,
  PipeRead,
  PipeWrite,
  RecordingLevel,
  ERROR,
  SourceError,
  SinkError,
  PipeError,
}
