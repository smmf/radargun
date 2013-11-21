# Some notes on running test on the FutureGrid

Scripts assume ~/radargun as the place to have radargun checked out.

# Quick sequence of steps to run tests (in portuguese)

## On the gateway

Connect:

    ssh futuregrid

Then on the remote shell:

    source ~/openstack/novarc
    module load euca2ools

Editar smf_start_vms para escolher as VMs slaves

Invocar . smf_main1.sh com o nome da imagem, p.e. 
  
    . smf_main1.sh smf/ubuntu-radargun-6

Quando o watch que corre, mostrar tudo o que preciso:  Ctrl-c

Depois:

    . smf_main2.sh

    ssh -i nmldkey.pem ubuntu@<IP do coordenador>

## On the coordinator node

    cd radargun

    git pull  etc. etc. 

    emacs bench.sh (para seleccionar testes

    emacs smf-scripts/environment.sh para editar o IP do coordenador!

    bash test-vms.sh

E ver que os ssh para slaves estão todos a funcionar. Se este passo correr
bem, saltar a sub-secção seguinte.

### SE ALGO FALHAR NO PASSO ANTERIOR

 É preciso manualmente criar slaves no gateway com:

    nova boot --image "smf/ubuntu-radargun-6" --flavor="4" --key-name nmldkey smfXXX <<--

APAGAR depois a instância que falhou

    nova delete <UUID>
    
Correr novamente a fase main2:

    . smf_main2.sh

Voltar ao coordenador com ssh se necessário

## Continuação

Adaptar os "fors/parâmetros" dos meus testes a correr

    emacs bench.sh 

Correr p.e. num  `screen -RD`:

     bash bench.sh

E passados os testes tenho os resultados na pasta `auto-results`.

# More info from email from Nuno Diegues

A partir daí vais fazer login:

ssh -i path-to-key-pub -A nmld@india.futuregrid.org


    source ~/openstack/novarc
    module load euca2ools

    nova list
    script: start_vms
    script: delete_vms

    nova flavor-list
    nova image-list

arranca um coordenador:

    nova boot --image "ndiegues/ubuntu-tree4" --flavor="2" --key-name nmldkey smf-cord

    nova list | grep 'smf-cord'
    ssh -i nmldkey.pem ubuntu@<ip>

colocar o java, etc.. necessários, e actualizar o .bashrc para apontar para o novo JAVA_HOME por ex.

exit da VM

e vamos guardar o teu estado da VM num snapshot:

    nova list | grep 'smf-cord'

para obter o id da VM

    nova image-create <id> <nome-do-teu-novo-snapshot>

e a partir daqui podes bootar novas VMS com o novo snapshot em vez do meu.


na pasta radargun, copiar btt-scripts para smf-scripts:

* benchmark-config-gen.sh --- modificar para produzir o XML --- copiar à mão para framework/src/main/resources/<nomedobenchmark>-config-generator.sh

* run-test.sh --- modificar os parâmetros de entrada e o BENC_DEFAULT (que é o comando passado ao benchmark-config-gen.sh

* environment.sh --- update à variável MASTER com o IP do coordenador; adaptar as variáveis dos geradores dos produtos, e no run-test.sh chamar os geradores correctos

no gateway do india, usar "check_ips" para obter lista de IPs

    scp do ficheiro "machines" para o coordenador:radargun/all_machines

## o meu ficheiro jgroups:

plugins/infinispan4/src/main/resources/jgroups/jgroups.xml

plugins/infinispan4/src/main/resources/ispn.xml



## dicas

boot ao coordenador com poucos recursos, género flavor 2

escolher as VMs para slaves no script start_vms

correr o start_vms

fazer nova list | grep '<nomedasvms>' e ver quando estão active

bash check_ips (produz para machines)

scp do ficheiro machines para coordenador:radargun/all_machines

ssh para o coordenador

cd radargun

git pull do meu código mais recente

bash test_vms.sh e ver que os ssh estão todos a bombar

vim bench.sh e adaptar os "fors/parâmetros" dos meus testes a correr

bash bench.sh num screen e passados os testes tenho os resultados na pasta auto-results


## mais dicas

"screen" inicia o screen

"ctrl+a+d" fecha o screen sem o matar

"screen -r" resume o screen

"exit" dentro do screen, mata o screen


# Scripts smf_

## smf_check_ips

    #!/bin/bash

    nova list | grep 'smf'  | grep -v cord | grep -o '10.[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}' > smf_machines

## smf_delete_vms

    #!/bin/bash

    for NODE in `echo {001..150}`
    do
        tmp=`printf "%03d" $NODE`
        echo "nova delete smf$tmp"
        nova delete smf$tmp
    done

## smf_machines

    10.1.2.7
    10.1.2.8
    10.1.2.9
    10.1.2.10
    10.1.2.15
    10.1.2.16
    10.1.2.17
    10.1.2.20
    10.1.2.21
    10.1.2.22
    10.1.2.24
    10.1.2.25

## smf_main1.sh

    #!/bin/bash

    echo "####################"
    echo ""
    echo "  * You should have edited smf_start_vms before running this script"
    echo ""
    echo "  * You should provide in \$1 the flavor. Currently \$1 = "$1
    echo ""
    echo "Press <ENTER> to continue. Ctrl-c to abort"
    read

    echo "Continuing..."

    nova boot --image "$1" --flavor="4" --key-name nmldkey smf-cord
    . smf_start_vms
    watch -n 1 "nova list | grep smf"

## smf_main2.sh

    #!/bin/bash

    . smf_check_ips
    cat smf_machines

    COORD=`nova list | grep smf-cord | cut -d= -f2 | cut -d, -f1`
    echo "will copy smf_machine to "$COORD
    echo "press <ENTER>"
    read

    scp -i nmldkey.pem smf_machines ubuntu@${COORD}:radargun/all_machines

## smf_start_vms

    #!/bin/bash

    for NODE in `echo {001..012}`
    do
        tmp=`printf "%03d" $NODE`
        echo "smf$tmp"
        #nova boot --image "smf/ubuntu-radargun-6" --flavor="4" --key-name nmldkey smf$tmp
        nova boot --image "$1" --flavor="4" --key-name nmldkey smf$tmp
        sleep 15
    done
