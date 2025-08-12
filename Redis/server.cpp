#include <sys/socket.h>
#include <netinet/in.h>
#include <iostream>
#include <stdlib.h>
#include <unistd.h>
#include <cstring>
#include <cassert>
#include <fcntl.h>
#include <stdio.h>

#include <poll.h>
#include "utils.h"
#include <vector>

enum {
    STATE_REQUEST = 0,
    STATE_RESPONSE = 1,
    STATE_END = 2, // mark the connection for deletion
};
struct Conn { // connection state 
    int fd = -1;
    uint32_t state = 0;
    size_t rbuf_size = 0;
    uint8_t rbuf[4 + k_max_msg];
    size_t wbuf_size = 0;
    size_t wbuf_sent = 0;
    uint8_t wbuf[4 + k_max_msg];
};


static void fd_set_nb(int fd) { // set fd to nonblocking
    errno = 0;
    int flags = fcntl(fd, F_GETFL, 0);
    if (errno) {
        die("fcntl error");
        return;
    }
    flags |= O_NONBLOCK;
    errno = 0;
    (void)fcntl(fd, F_SETFL, flags);
    if (errno) {
        die("fcntl error");
    }
}
static void conn_put(std::vector<Conn*>& fd2conn, struct Conn* conn) {
    if (fd2conn.size() <= (size_t)conn->fd) {
        fd2conn.resize(conn->fd + 1);
    }
    fd2conn[conn->fd] = conn;
}

static int32_t accept_new_conn(std::vector<Conn*>& fd2conn, int fd) {
    // accept
    struct sockaddr_in client_addr = {};
    socklen_t socklen = sizeof(client_addr);
    int connfd = accept(fd, (struct sockaddr*)&client_addr, &socklen);
    if (connfd < 0) {
        msg("accept() error");
        return -1;
        // error
    }
    // set the new connection fd to nonblocking mode
    fd_set_nb(connfd);
    // creating the struct Conn
    struct Conn* conn = (struct Conn*)malloc(sizeof(struct Conn));
    if (!conn) {
        close(connfd);
        return -1;
    }
    conn->fd = connfd;
    conn->state = STATE_REQUEST;
    conn->rbuf_size = 0;
    conn->wbuf_size = 0;
    conn->wbuf_sent = 0;
    conn_put(fd2conn, conn);
    return 0;
}

static bool try_flush_buffer(Conn* conn) {
    ssize_t rv = 0;
    do {
        size_t remain = conn->wbuf_size - conn->wbuf_sent;
        rv = write(conn->fd, &conn->wbuf[conn->wbuf_sent], remain);
        if (rv < 0 && errno == EAGAIN) {
        // got EAGAIN, stop.
            return false;
        }
        if (rv < 0) {
            msg("write() error");
            conn->state = STATE_END;
            return false;
        }
        conn->wbuf_sent += (size_t)rv;
        assert(conn->wbuf_sent <= conn->wbuf_size);
        if (conn->wbuf_sent == conn->wbuf_size) {
        // response was fully sent, change state back
            conn->state = STATE_REQUEST;
            conn->wbuf_sent = 0;
            conn->wbuf_size = 0;
            return false;
        }
        // still got some data in wbuf, could try to write again
        return true;
    } while (true);
}
static void state_response(Conn* conn) {
    while (try_flush_buffer(conn)) {}
}

static bool try_one_request(Conn* conn) {
// try to parse a request from the buffer
    if (conn->rbuf_size < 4) {
    // not enough data in the buffer. Will retry in the next iteration
        return false;
    }
    uint32_t len = 0;
    memcpy(&len, &conn->rbuf[0], 4);
    if (len > k_max_msg) {
        msg("too long");
        conn->state = STATE_END;
        return false;
    }
    if (4 + len > conn->rbuf_size) {
    // not enough data in the buffer. Will retry in the next iteration
        return false;
    }
    // got one request, do something with it
    printf("client says: %.*s\n", len, &conn->rbuf[4]);
    // generating echoing response
    memcpy(&conn->wbuf[0], &len, 4);
    memcpy(&conn->wbuf[4], &conn->rbuf[4], len);
    conn->wbuf_size = 4 + len;
    // remove the request from the buffer.
    // note: frequent memmove is inefficient.
    // note: need better handling for production code.
    size_t remain = conn->rbuf_size - 4 - len;
    if (remain) {
        memmove(conn->rbuf, &conn->rbuf[4 + len], remain);
    }
    conn->rbuf_size = remain;
    // change state
    conn->state = STATE_RESPONSE;
    state_response(conn);
    // continue the outer loop if the request was fully processed
    return (conn->state == STATE_REQUEST);
}

static bool try_fill_buffer(Conn* conn) {
    // try to fill the buffer
    assert(conn->rbuf_size < sizeof(conn->rbuf));
    ssize_t rv = 0;
    do {
        size_t cap = sizeof(conn->rbuf) - conn->rbuf_size;
        rv = read(conn->fd, &conn->rbuf[conn->rbuf_size], cap);
    } while (rv < 0 && errno == EINTR);
    if (rv < 0 && errno == EAGAIN) {
    // got EAGAIN, stop.
        return false;
    }
    if (rv < 0) {
        msg("read() error");
        conn->state = STATE_END;
        return false;
    }
    if (rv == 0) {
        if (conn->rbuf_size > 0) {
            msg("unexpected EOF");
        }
        else {
            msg("EOF");
        }
        conn->state = STATE_END;
        return false;
    }
    conn->rbuf_size += (size_t)rv;
    assert(conn->rbuf_size <= sizeof(conn->rbuf) - conn->rbuf_size);
    // Try to process requests one by one.
    // Why is there a loop? Please read the explanation of "pipelining".
    while (try_one_request(conn)) {}
    return (conn->state == STATE_REQUEST);
}
static void state_request(Conn* conn) {
    while (try_fill_buffer(conn)) {}
}

static void connection_io(Conn* conn) {
    if (conn->state == STATE_REQUEST) {
        state_request(conn);
    }
    else if (conn->state == STATE_RESPONSE) {
        state_response(conn);
    }
    else {
        assert(0); // not expected
    }
}

static int32_t one_request(int connfd) {
    // 4 bytes header
    char rbuf[4 + k_max_msg + 1];
    errno = 0;
    int32_t err = read_full(connfd, rbuf, 4);
    if (err) {
        if (errno == 0) {
            msg("EOF");
        }
        else {
            msg("read() error");
        }
        return err;
    }
    uint32_t len = 0;
    memcpy(&len, rbuf, 4);
    // assume little endian
    if (len > k_max_msg) {
        msg("too long");
        return -1;
    }
    // request body
    err = read_full(connfd, &rbuf[4], len);
    if (err) {
        msg("read() error");
        return err;
    }
    // do something
    rbuf[4 + len] = '\0';
    printf("client says: %s\n", &rbuf[4]);
    // reply using the same protocol
    const char reply[] = "world";
    char wbuf[4 + sizeof(reply)];
    len = (uint32_t)strlen(reply);

    memcpy(wbuf, &len, 4);
    memcpy(&wbuf[4], reply, len);
    return write_all(connfd, wbuf, 4 + len);
}

int main()
{
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) {
        die("socket()");
    }
    int val = 1;
    setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &val, sizeof(val));

    // bind
    struct sockaddr_in addr = {};
    addr.sin_family = AF_INET; // ipv4
    addr.sin_port = ntohs(1234); // port 1234
    addr.sin_addr.s_addr = ntohl(0); // wildcard address 0.0.0.0
    int rv = bind(fd, (const sockaddr*)&addr, sizeof(addr)); // bind socket to address 
    if (rv) {
        die("bind()");
    }

    // listen
    rv = listen(fd, SOMAXCONN); // listen with SOMAXCONN max connections
    if (rv) {
        die("listen()");
    }

    // a map of all client connections, keyed by fd
    std::vector<Conn*> fd2conn;
    // set the listen fd to nonblocking mode
    fd_set_nb(fd);

    // the event loop
    std::vector<struct pollfd> poll_args;
    while (true) {
    // prepare the arguments of the poll()
        poll_args.clear();
        // for convenience, the listening fd is put in the first position
        struct pollfd pfd = { fd, POLLIN, 0 };
        poll_args.push_back(pfd);
        // connection fds
        for (Conn* conn : fd2conn) {
            if (!conn) {
                continue;
            }
            struct pollfd pfd = {};
            pfd.fd = conn->fd;
            pfd.events = (conn->state == STATE_REQUEST) ? POLLIN : POLLOUT;
            pfd.events = pfd.events | POLLERR;
            poll_args.push_back(pfd);
        }
        // poll for active fds
        // the timeout argument doesn't matter here
        int rv = poll(poll_args.data(), (nfds_t)poll_args.size(), 1000);
        if (rv < 0) {
            die("poll");
        }
            // process active connections
        for (size_t i = 1; i < poll_args.size(); ++i) {
            if (poll_args[i].revents) {
                Conn* conn = fd2conn[poll_args[i].fd];
                connection_io(conn);
                if (conn->state == STATE_END) {
                // client closed normally, or something bad happened.
                // destroy this connection
                    fd2conn[conn->fd] = NULL;
                    (void)close(conn->fd);
                    free(conn);
                }
            }
        }
        // try to accept a new connection if the listening fd is active
        if (poll_args[0].revents) {
            (void)accept_new_conn(fd2conn, fd);
        }
    }
    return 0;
}