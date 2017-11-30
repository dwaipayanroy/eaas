#!/bin/bash
# Given a list of words, this code computes the kNNs for each of the word and stores them in a Lucene index

# Input:
# 3. coll:
# 5. k: the value of k for kNN
# 7. nnIndexBasedir: 
# 4. nnPath:

### + Set variables:

coll="gov2"

k="100"

nnIndexBasedir="/home/dwaipayan/nnIndex/"

nnPath="/home/dwaipayan/gov2_size-200_window-5_negative-5_cbow_iter-3.100nn"
### - Set variables:

cd ..

# ant -q

# Storing them in a Lucene index:
prop_name="nn-index-init.properties"

cat > $prop_name <<EOF
coll=$coll

k=$k

nnPath=$nnPath

nnIndexBasedir=$nnIndexBasedir

EOF

# ant

java -cp dist/wvecs4j.jar wvec.NNIndexer $prop_name

