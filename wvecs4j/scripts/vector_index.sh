#!/bin/bash

### + Set variables:

# Set the following variables:
## 1. coll: The name of the collecion with which the vector index will be saved
## 2. wvecPath:
## 7. wvecsIndexBasedir:

coll="gov2-1"

#wvecPath=$wvecsPath"/"$coll".vec"
wvecPath="/user1/faculty/cvpr/irlab/collections/w2v-trained/gov2/gov2_size-200_window-5_negative-5_cbow_iter-3.vec"

wvecsIndexBasedir="/user1/faculty/cvpr/irlab/collections/w2v-trained-index/"

cd ..

ant -q

if [ $? -eq 0 ]
then
    echo "*** Build successful ***"
    prop_name="init.properties"

    cat > $prop_name <<EOF
coll=$coll

wvecsIndexBasedir=$wvecsIndexBasedir

wvecPath=$wvecPath

EOF
    echo "Indexing word vectors."
    java -cp dist/wvecs4j.jar wvec.WordVecsIndexer $prop_name
    echo "Completed indexing word vectors."

else
    echo "*** Build failed ***" 
    exit 2
fi

