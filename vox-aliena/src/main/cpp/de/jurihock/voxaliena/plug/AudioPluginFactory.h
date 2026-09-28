#pragma once

#include <voxaliena/Header.h>

#include <voxaliena/etc/JNA.h>

jna bool voxaliena_plugin_open(const char* name, jna_callback* callback, jna_pointer* pointer, jna_result* result);
jna bool voxaliena_plugin_setup(int input, int output, int samplerate, int blocksize, int channels, jna_pointer* pointer, jna_result* result);
jna bool voxaliena_plugin_set(const char* param, const char* value, jna_pointer* pointer, jna_result* result);
jna bool voxaliena_plugin_start(jna_pointer* pointer, jna_result* result);
jna bool voxaliena_plugin_start_recording(const char* path, jna_pointer* pointer, jna_result* result);
jna float voxaliena_plugin_level(jna_pointer* pointer, jna_result* result);
jna bool voxaliena_plugin_stop(jna_pointer* pointer, jna_result* result);
jna bool voxaliena_plugin_close(jna_pointer* pointer, jna_result* result);
