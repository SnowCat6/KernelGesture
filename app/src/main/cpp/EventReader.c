#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <linux/input.h>
#include <string.h>
#include <stdio.h>
#include<pthread.h>

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
};
static struct ThreadArg args[20];

void* readInput(void *arg);

int main(int argc, char **argv)
{
    setbuf(stdout, NULL);
    setbuf(stderr, NULL);

    printf("Event reader started!\n");

    int i;
    for (i = 1; i < argc && i < 20; i++)
    {
        args[i].device = argv[i];

        pthread_t tid = 0;
        int err = pthread_create(&tid, NULL, &readInput, &args[i]);
        if (err != 0) printf("can't create thread :[%s]\n", strerror(err));
    }

    while(getchar() != ' ');

    return 0;
}
void* readInput(void *arg)
{
    struct ThreadArg* input = (struct ThreadArg*)arg;

    int fd = 0;
    // Write a key to the keyboard buffer
    if((fd = open(input->device, O_RDONLY)) <= 0){
        fprintf(stdout, "Error open: %s\n", input->device);
        return NULL;
    }

    fprintf(stdout, "Opened: %s\n", input->device);

    struct input_event event, last_event;
    memset(&last_event, 0, sizeof(last_event));

    while(1)
    {
        memset(&event, 0, sizeof(event));
        ssize_t nRead = read(fd, &event, sizeof(event));
        if (nRead == (ssize_t)-1){
            if (errno == EINTR) continue;
            printf("Failed read input device:%s\n", input->device);
            break;
        }
        if (event.type != EV_KEY) continue;

        struct timeval diff;
        timersub(&event.time, &last_event.time, &diff);

        fprintf(stderr, "[%8ld.%06ld] %s: %-12.12s %-20.20s  %s\n",
               diff.tv_sec, diff.tv_usec,
               input->device,
               "EV_KEY",
               get_label(key_labels, event.code),
               get_label(key_value_labels, event.value));

        memcpy(&last_event, &event, sizeof(last_event));
    }
    close(fd);
}