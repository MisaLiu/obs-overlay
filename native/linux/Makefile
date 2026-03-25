CC = gcc
CFLAGS = -fPIC -Wall -O2
LDFLAGS = -shared -ldl

TARGET = libglx_hook.so
SRC = glx_hook.c

all: $(TARGET)

$(TARGET): $(SRC)
	$(CC) $(CFLAGS) $(LDFLAGS) -o $(TARGET) $(SRC)

clean:
	rm -f $(TARGET)

.PHONY: all clean
