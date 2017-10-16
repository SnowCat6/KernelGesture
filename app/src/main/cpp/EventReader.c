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

void onEvent(int nfds, struct input_event event);

struct ThreadArg{
    const char* device;
    int fd;
    struct input_event last_event;
};
static nfds_t nfds = 0;
static struct pollfd ufds[MAX_DEVICE_INPUT];
static struct ThreadArg thisEvents[MAX_DEVICE_INPUT];

void sig_handler(int signo);

int main(int argc, char **argv)
{
    setbuf(stdout, NULL);
    setbuf(stderr, NULL);

    signal(SIGINT, sig_handler);
    signal(SIGKILL, sig_handler);
    signal(SIGHUP, sig_handler);
    signal(SIGQUIT, sig_handler);
    signal(SIGTERM, sig_handler);
    signal(SIGTSTP, sig_handler);

    printf("Event reader started\n");

    for (int i = 1; i < argc && nfds < MAX_DEVICE_INPUT; i++)
    {
        thisEvents[nfds].device = argv[i];
        thisEvents[nfds].fd = open(thisEvents[nfds].device, O_RDONLY);
        if (thisEvents[nfds].fd <= 0) {
            fprintf(stdout, "Error open: %s\n", thisEvents[nfds].device);
            continue;
        }

        ufds[nfds].fd = thisEvents[nfds].fd;
        ufds[nfds].events = POLLIN;

        fprintf(stdout, "Opened: %s\n", thisEvents[nfds].device);
        ++nfds;
    }

    if (nfds == 0){
        fprintf(stdout, "No input devices found\n");
        exit(1);
    }

    int bOK = 1;
    struct input_event event;

    while(bOK)
    {
        poll(ufds, nfds, -1);
        for(int i = 0; i < nfds; i++)
        {
            if (ufds[i].revents == 0) continue;
            if((ufds[i].revents & POLLIN) == 0) continue;

            bOK = read(ufds[i].fd, &event, sizeof(event)) == sizeof(event);
            if (!bOK) break;

            onEvent(i, event);
        }
    }

    return 0;
}

void onEvent(int nfds, struct input_event event)
{
    if (event.type != EV_KEY) return;
    struct ThreadArg *thisEvent = &thisEvents[nfds];

    struct timeval diff;
    timersub(&event.time, &thisEvent->last_event.time, &diff);

    fprintf(stderr, "[%8ld.%06ld] %s: %-12.12s %-20.20s  %s\n",
            diff.tv_sec, diff.tv_usec,
            thisEvent->device,
            "EV_KEY",
            get_label(key_labels, event.code),
            get_label(key_value_labels, event.value));

    memcpy(&thisEvent->last_event, &event, sizeof(event));
}

void sig_handler(int signo)
{
    printf("Event reader exit\n");
    for(int i = 0; i < nfds; i++){
        close(thisEvents->fd);
    }
    exit(0);
}