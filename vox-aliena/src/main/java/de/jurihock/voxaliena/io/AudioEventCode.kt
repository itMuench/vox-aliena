package de.jurihock.voxaliena.io

enum class AudioEventCode {
  INFO,
  WARNING,
  SourceOverflow,
  SourceOverrun,
  SinkUnderflow,
  SinkUnderrun,
  PipeRead,
  PipeWrite,
  ERROR,
  SourceError,
  SinkError,
  PipeError,
}
