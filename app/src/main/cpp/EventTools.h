//
// Created by Костя on 18.10.2017.
//

#ifndef KERNELGESTURE_TOOLS_H
#define KERNELGESTURE_TOOLS_H

#include "EventReader.h"

void wakeLock();
void wakeUnlock();
void sig_handler(int signo);
void timer_handler(int signo);

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


void sig_handler(int signo)
{
    closeAll();
    exit(0);
}

void timer_handler(int sig){
    wakeUnlock();
}

void initIO(){
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
}

#endif //KERNELGESTURE_TOOLS_H
