// utils.h
#ifndef UTILS_H
#define UTILS_H

#include <sys/socket.h>
#include <netinet/in.h>
#include <iostream>
#include <stdlib.h>
#include <unistd.h>
#include <cstring>
#include <cassert>

#include <stdio.h>

static void msg(const char* msg) {
    fprintf(stderr, "%s\n", msg);
}

void die(const char* s) {
    perror(s);
    exit(1);
}

const size_t k_max_msg = 4096;


static int32_t read_full(int fd, char* buf, size_t n) {
    while (n > 0) {
        ssize_t rv = read(fd, buf, n);
        if (rv <= 0) {
            return -1; // error, or unexpected EOF
        }
        assert((size_t)rv <= n);
        n -= (size_t)rv;
        buf += rv;
    }
    return 0;
}

static int32_t write_all(int fd, const char* buf, size_t n) {
    while (n > 0) {
        ssize_t rv = write(fd, buf, n);
        if (rv <= 0) {
            return -1; // error
        }
        assert((size_t)rv <= n);
        n -= (size_t)rv;
        buf += rv;
    }
    return 0;
}

#endif // UTILS_H
