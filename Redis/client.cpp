#include <sys/socket.h>
#include <netinet/in.h>
#include <iostream>
#include <stdlib.h>
#include <unistd.h>
#include <cstring>

#include "utils.h"

static int32_t send_req(int fd, const char* text) {
    uint32_t len = (uint32_t)strlen(text);
    if (len > k_max_msg) {
        return -1;
    }
    char wbuf[4 + k_max_msg];
    memcpy(wbuf, &len, 4); // little endian assumption
    memcpy(&wbuf[4], text, len);

    if (int32_t err = write_all(fd, wbuf, 4 + len)) {
        return err;
    }
    return 0;
}

static int32_t read_res(int fd) {
    char rbuf[4 + k_max_msg + 1];
    uint32_t len;

    errno = 0;
    int32_t err = read_full(fd, rbuf, 4); // read length header
    if (err) {
        if (errno == 0) {
            msg("EOF");
        }
        else {
            msg("read() error");
        }
        return err;
    }

    memcpy(&len, rbuf, 4); // little endian assumption
    if (len > k_max_msg) {
        msg("too long");
        return -1;
    }

    // read the payload
    err = read_full(fd, &rbuf[4], len);
    if (err) {
        msg("read() error");
        return err;
    }

    rbuf[4 + len] = '\0';
    printf("server says: %s\n", &rbuf[4]);
    return 0;
}

int main() {
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) {
        die("socket()");
    }

    struct sockaddr_in addr = {};
    addr.sin_family = AF_INET;
    addr.sin_port = ntohs(1234);
    addr.sin_addr.s_addr = ntohl(INADDR_LOOPBACK);

    int rv = connect(fd, (const struct sockaddr*)&addr, sizeof(addr));
    if (rv) {
        die("connect");
    }

    // Multiple requests (pihepelined style)
    const char* queries[] = { "hello0001", "hello2", "hello3" };
    for (size_t i = 0; i < 3; i++) {
        if (int32_t err = send_req(fd, queries[i])) {
            goto L_DONE;
        }
    }
    for (size_t i = 0; i < 3; i++) {
        if (int32_t err = read_res(fd)) {
            goto L_DONE;
        }
    }

L_DONE:
    close(fd);
    return 0;
}
