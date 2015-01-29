#!/bin/bash

if [ "$#" -lt 1 ]
then
	echo "Usage: $0 INSTALL_DIR (where INSTALL_DIR is the installation directory)"
	exit 1
fi

INSTALL_DIR=$1
VERSION=1.1.6

if [ -d "$1" ]
then

	echo "Downloading prebuilt wsn-device-utils jar files to $INSTALL_DIR..."

	wget -q -O $INSTALL_DIR/wdu-macreader-cli-$VERSION.jar "https://maven.itm.uni-luebeck.de/service/local/artifact/maven/redirect?r=releases&g=de.uniluebeck.itm.wsn-device-utils&a=macreader-cli&v=$VERSION&e=jar&c=onejar"
	wget -q -O $INSTALL_DIR/wdu-macwriter-cli-$VERSION.jar "https://maven.itm.uni-luebeck.de/service/local/artifact/maven/redirect?r=releases&g=de.uniluebeck.itm.wsn-device-utils&a=macwriter-cli&v=$VERSION&e=jar&c=onejar"
	wget -q -O $INSTALL_DIR/wdu-listener-cli-$VERSION.jar  "https://maven.itm.uni-luebeck.de/service/local/artifact/maven/redirect?r=releases&g=de.uniluebeck.itm.wsn-device-utils&a=listener-cli&v=$VERSION&e=jar&c=onejar"
	wget -q -O $INSTALL_DIR/wdu-observer-cli-$VERSION.jar  "https://maven.itm.uni-luebeck.de/service/local/artifact/maven/redirect?r=releases&g=de.uniluebeck.itm.wsn-device-utils&a=observer-cli&v=$VERSION&e=jar&c=onejar"
	wget -q -O $INSTALL_DIR/wdu-flasher-cli-$VERSION.jar   "https://maven.itm.uni-luebeck.de/service/local/artifact/maven/redirect?r=releases&g=de.uniluebeck.itm.wsn-device-utils&a=flasher-cli&v=$VERSION&e=jar&c=onejar"
	wget -q -O $INSTALL_DIR/wdu-gui-$VERSION.jar           "https://maven.itm.uni-luebeck.de/service/local/artifact/maven/redirect?r=releases&g=de.uniluebeck.itm.wsn-device-utils&a=gui&v=$VERSION&e=jar&c=sources"

	echo "Creating bash scripts..."
	echo -e "#!/bin/bash\nDIR=\`dirname \$0\`\nexec java -jar \$DIR/wdu-macreader-cli-$VERSION.jar \$*" > $INSTALL_DIR/wdu-readmac.sh
	echo -e "#!/bin/bash\nDIR=\`dirname \$0\`\nexec java -jar \$DIR/wdu-macwriter-cli-$VERSION.jar \$*" > $INSTALL_DIR/wdu-writemac.sh
	echo -e "#!/bin/bash\nDIR=\`dirname \$0\`\nexec java -jar \$DIR/wdu-listener-cli-$VERSION.jar \$*"  > $INSTALL_DIR/wdu-listen.sh
	echo -e "#!/bin/bash\nDIR=\`dirname \$0\`\nexec java -jar \$DIR/wdu-observer-cli-$VERSION.jar \$*"  > $INSTALL_DIR/wdu-observe.sh
	echo -e "#!/bin/bash\nDIR=\`dirname \$0\`\nexec java -jar \$DIR/wdu-flasher-cli-$VERSION.jar \$*"   > $INSTALL_DIR/wdu-flash.sh
	echo -e "#!/bin/bash\nDIR=\`dirname \$0\`\nexec java -jar \$DIR/wdu-gui-$VERSION.jar \$*"           > $INSTALL_DIR/wdu-gui.sh

	chmod +x $INSTALL_DIR/wdu-readmac.sh
	chmod +x $INSTALL_DIR/wdu-writemac.sh
	chmod +x $INSTALL_DIR/wdu-listen.sh
	chmod +x $INSTALL_DIR/wdu-observe.sh
	chmod +x $INSTALL_DIR/wdu-flash.sh
	chmod +x $INSTALL_DIR/wdu-gui.sh

	echo -e "Done. Please add '$INSTALL_DIR' to your \$PATH environment variable and run either one of wdu-readmac, wdu-writemac, wdu-listen, wdu-observe, wdu-flash or wdu-gui"

else
	echo "The given INSTALL_DIR $1 is not a valid folder"
	exit 1
fi