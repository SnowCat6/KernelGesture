
#include "EventReader.h"
#include "EventTools.h"

int addInput(const char* deviceName);
int readInputs();
void onEvent(int inputNdx, struct input_event event);

int main(int argc, char **argv)
{
    initIO();
    printf("Event reader started\n");

    eventInputNdx = 0;
    for (int i = 1; i < argc; i++){
        addInput(argv[i]);
    }

    if (eventInputNdx < 1){
        fprintf(stderr, "No input devices found\n");
        closeAll();
        exit(1);
    }

    readInputs();

    closeAll();
    return 0;
}

int readInputs()
{
    int bOK = 1;
    struct input_event event;

    while(bOK)
    {
        poll(eventInputPuuls, eventInputNdx, -1);
        for(int i = 0; i < eventInputNdx; i++)
        {
            if (eventInputPuuls[i].revents == 0) continue;
            if((eventInputPuuls[i].revents & POLLIN) == 0) continue;

            bOK = read(eventInputPuuls[i].fd, &event, sizeof(event)) == sizeof(event);
            if (!bOK){
                fprintf(stderr, "Event read failed\n");
                break;
            }

            onEvent(i, event);
        }
    }
    return bOK;
}

int addInput(const char* deviceName)
{
    if (eventInputNdx == MAX_DEVICE_INPUT) return 0;

    int fd = open(deviceName, O_RDONLY);
    if (fd <= 0) {
        fprintf(stderr, "Error open: %s\n", deviceName);
        return 0;
    }

    eventInputPuuls[eventInputNdx].fd = fd;
    thisEvents[eventInputNdx].device = deviceName;
    eventInputPuuls[eventInputNdx].events = POLLIN;
    ++eventInputNdx;

    return 1;
}
void onEvent(int inputNdx, struct input_event event)
{
    if (event.type != EV_KEY) return;

    struct ThreadArg *thisEvent = &thisEvents[inputNdx];
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

void closeAll()
{
    for(int i = 0; i < eventInputNdx; i++){
        close(eventInputPuuls->fd);
    }
    eventInputNdx = 0;
    wakeUnlock();
    fprintf(stderr, "Event reader closed\n");
}
