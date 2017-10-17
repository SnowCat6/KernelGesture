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

void onEvent(int nfds, struct input_event event);

struct ThreadArg{
    const char* device;
    struct input_event last_event;
};
static nfds_t nfds = 0;
static struct pollfd ufds[MAX_DEVICE_INPUT];
static struct ThreadArg thisEvents[MAX_DEVICE_INPUT];

static int bWakeLock = 0;
static const char* lockName = "EventReader";
static struct sigaction sact;
static int timerId = 0;

void sig_handler(int signo);
void closeAll();
void wakeLock();
void wakeUnlock();
void timer_handler(int signo);

int main(int argc, char **argv)
{
    setbuf(stdout, NULL);
    setbuf(stderr, NULL);

    //  KILL SIGNALS
    signal(SIGINT, sig_handler);
    signal(SIGKILL, sig_handler);
    signal(SIGHUP, sig_handler);
    signal(SIGQUIT, sig_handler);
    signal(SIGTERM, sig_handler);
    signal(SIGTSTP, sig_handler);
    //  Parent process was killed
    prctl(PR_SET_PDEATHSIG, SIGHUP);

    sigemptyset(&sact.sa_mask);
    sact.sa_flags = 0;
    sact.sa_handler = timer_handler;
    sigaction(SIGALRM, &sact, NULL);

    printf("Event reader started\n");

    nfds = 0;
    for (int i = 1; i < argc && nfds < MAX_DEVICE_INPUT; i++)
    {
        thisEvents[nfds].device = argv[i];
        ufds[nfds].fd = open(thisEvents[nfds].device, O_RDONLY);
        if (ufds[nfds].fd <= 0) {
            fprintf(stderr, "Error open: %s\n", thisEvents[nfds].device);
            continue;
        }

        ufds[nfds].events = POLLIN;
        ++nfds;
    }

    if (nfds < 1){
        fprintf(stderr, "No input devices found\n");
        closeAll();
        exit(1);
    }

    int bOK = 1;
    struct input_event event;

    while(bOK)
    {
        poll(ufds, nfds, 5*1000);
        for(int i = 0; i < nfds; i++)
        {
            if (ufds[i].revents == 0) continue;
            if((ufds[i].revents & POLLIN) == 0) continue;

            bOK = read(ufds[i].fd, &event, sizeof(event)) == sizeof(event);
            if (!bOK){
                fprintf(stderr, "Event read failed\n");
                break;
            }

            onEvent(i, event);
        }
    }
    closeAll();
    return 0;
}

int echo(const char* fileName, const char* message)
{
    int fd = open(fileName, O_RDWR, S_IWUSR);
    if (fd == -1) return 0;

    write(fd, message, strlen(message));
    close(fd);

    return 1;
}

void vibrate(int nTime){
    char strTime[12];
    sprintf(strTime, "%d", nTime);
    if (echo("/sys/devices/virtual/timed_output/vibrator/enable", strTime) == 0){
        fprintf(stderr, "Vibrate failed\n");
    };
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
    closeAll();
    exit(0);
}

void closeAll()
{
    for(int i = 0; i < nfds; i++){
        close(ufds->fd);
    }
    nfds = 0;
    wakeUnlock();
    fprintf(stderr, "Event reader exit\n");
}

void wakeLock()
{
    if (bWakeLock == 1) return;

    if (echo("/sys/power/wake_lock", lockName) == 0){
        fprintf(stderr, "Wake lock failed\n");
        return;
    }
    bWakeLock = 1;
    fprintf(stderr, "Wake lock\n");
    timerId = alarm(1);
}
void wakeUnlock()
{
    if (bWakeLock == 0) return;
    if (echo("/sys/power/wake_unlock", lockName) == 0){
        fprintf(stderr, "Wake unlock failed\n");
        return;
    }
    bWakeLock = 0;
    fprintf(stderr, "Wake unlock\n");
}
void timer_handler(int sig){
    wakeUnlock();
}