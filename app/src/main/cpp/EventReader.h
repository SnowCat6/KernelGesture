//
// Created by Костя on 18.10.2017.
//

#ifndef KERNELGESTURE_EVENTREADER_H_H
#define KERNELGESTURE_EVENTREADER_H_H

#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <linux/input.h>
#include <string.h>
#include <stdio.h>
#include<pthread.h>
#include <stdint.h>
#include <dirent.h>
#include <sys/inotify.h>
#include <sys/poll.h>
#include <time.h>
#include <signal.h>
#include <sys/prctl.h>

#define MAX_DEVICE_INPUT 10
#define LABEL(constant) { #constant, constant }
#define LABEL_END { NULL, -1 }

struct label {
    const char *name;
    int         value;
};

static struct label key_value_labels[] = {
        { "UP", 0 },
        { "DOWN", 1 },
        { "REPEAT", 2 },
        LABEL_END,
};
#include "input.h-labels.h"

static const char *get_label(const struct label *labels, int value)
{
    while(labels->name && value != labels->value) {
        labels++;
    }
    return labels->name;
}

struct ThreadArg{
    const char* device;
    struct input_event last_event;
};

static nfds_t eventInputNdx = 0;
static struct pollfd eventInputPuuls[MAX_DEVICE_INPUT];
static struct ThreadArg thisEvents[MAX_DEVICE_INPUT];

static int bWakeLock = 0;
static const char* lockName = "EventReader";
static struct sigaction sact;
static int timerId = 0;

void closeAll();


#endif //KERNELGESTURE_EVENTREADER_H_H
