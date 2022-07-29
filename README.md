# Performance evaluation of the software EasySMPC

> This repository allows to reproduce the performance evaluation of the software [EasySMPC](https://github.com/prasser/easy-smpc) in a dockerized form with the e-mail server  [iRedMail](https://www.iredmail.org/). The steps are described below.
> The result of a run of the evaluation are stored in the folder `results`
---

## Steps to reproduce the performance evaluation
1. Clone this repository with the command `clone https://github.com/fnwirth/easy-smpc-performance-evaluation`.
1. Build the program with the command `mvn clean package`. The two warnings regarding the import of easy-smpc.jar can be ignored.
1. Create the docker image by changing to the folder *docker* and executing the script `createDockerImage.sh` or `createDockerImage.bat` respectively
1. Start the docker image with the script `startEvaluation.sh` or `startEvaluation.bat`. To change the network delay, the parameter *TC_DELAY_MS* can adapted before starting the script.
1. The performance evaluation is running and can be inspected with the command `docker logs easy-eval`. The results can be accessed in the file */root/easy-smpc/result.csv* within the container e.g. by copying the file to the host with the command `docker cp easy-eval:/root/easy-smpc/result.csv .`

## Troubleshooting
### Latency created with *tc*
The latency within the docker container is creates with the program *tc*. On startup this program may fail to start. If this is the case, the docker logs show the error message ` Specified qdisc not found`. Solving this issue is dependent of your host OS:
* **Linux**: Please make sure the kernel module *sch_netem* is loaded, e.g. by installing the package *kernel-modules-extra* provided by different Linux distributions.
* **Windows**: Please deactivate the use of WSL 2 engine as described [here](https://stackoverflow.com/questions/68176812/docker-traffic-control-tc-specified-qdisc-not-found).

### Required resources
10 gigabyte of RAM for the docker container are recommended.

## Contact
See github [README page](https://github.com/prasser/easy-smpc/edit/master/README.md) of EasySMPC

## License
This software is licensed under the Apache License 2.0. The full text is accessible in the LICENSE file. EasySMPC itself has several dependencies whose license files are listed in the github [README page](https://github.com/prasser/easy-smpc/edit/master/README.md).
The dockerfile uses the docker image provided by iRedMail. See the [documentation](https://github.com/iredmail/dockerized) of the image for license details.
