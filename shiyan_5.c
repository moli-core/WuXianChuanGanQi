#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#include <linux/input.h>

//设置LED灯的状态：value=1 亮；value=0 灭
void writeLed(const char *path, int value){
    int fd = open(path, O_RDWR);
	char ledStat;
    if(fd >= 0){
		sprintf(&ledStat, "%d", value);
		write(fd, &ledStat, 1);
		close(fd);
	}
    else{
        printf("Can not write led : %s\r\n",path);
    }
}

//读取LED灯的状态
int readLed(const char *path){
    int fd;
    int v = 0;

    fd = open(path, O_RDONLY);
    if(fd >= 0){
        char buffer[20];
        if(read(fd, buffer, sizeof(buffer)) > 0){
            sscanf(buffer, "%d\n", &v);
        }
        close(fd);
    }
    return v;
}


int main(int argc, char *argv[]){
	int keyfd = open("/dev/input/event0", O_RDWR);

	while(1){
		struct input_event key;

	    read(keyfd, &key, sizeof(key));
	    printf("key.code == %d\n", key.code);
	    printf("key.value == %d\n", key.value);
		
		if((key.value == 1) && (key.code != 0)){
			switch(key.code){
			case 68:	//KEY4 PRESS_DOWN
			{
				int led1 = readLed("/sys/devices/platform/leds-gpio/leds/led1/brightness");
				if(led1 == 0)
					writeLed("/sys/devices/platform/leds-gpio/leds/led1/brightness", 1);
				else
					writeLed("/sys/devices/platform/leds-gpio/leds/led1/brightness", 0);
				break;
			}
			case 67:	//KEY3 PRESS_DOWN
			{
				int led2 = readLed("/sys/devices/platform/leds-gpio/leds/led2/brightness");
				if(led2 == 0)
					writeLed("/sys/devices/platform/leds-gpio/leds/led2/brightness", 1);
				else
					writeLed("/sys/devices/platform/leds-gpio/leds/led2/brightness", 0);			
				break;
			}
			case 66:	//KEY2 PRESS_DOWN
			{
				int led3 = readLed("/sys/devices/platform/leds-gpio/leds/led3/brightness");
				if(led3 == 0)
					writeLed("/sys/devices/platform/leds-gpio/leds/led3/brightness", 1);
				else
					writeLed("/sys/devices/platform/leds-gpio/leds/led3/brightness", 0);			
				break;
			}
			case 65:	//KEY1 PRESS_DOWN
			{
				int led4 = readLed("/sys/devices/platform/leds-gpio/leds/led4/brightness");
				if( led4 == 0)
					writeLed("/sys/devices/platform/leds-gpio/leds/led4/brightness", 1);
				else
					writeLed("/sys/devices/platform/leds-gpio/leds/led4/brightness", 0);			
				break;
			}
			}
		}
 	}

	return 0;
}
