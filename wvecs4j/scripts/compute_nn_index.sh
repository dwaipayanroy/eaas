#!/bin/bash
# Given a list of words, this code computes the kNNs for each of the word and stores them in a Lucene index

# Input:
# 1. wvecsIndexBasedir: 
# 2. wvecsBasedir: where the .vec and .bin files are stored
# 3. nnBasedir: where all the nns are stored
# 3. coll:
# 4. queryTermList: list of terms for which the NNs are to be calculated and indexed
# 5. k: the value of k for kNN
# 6. tempNNPath: temp. path where the NNs will be stored before indexing
# 7. nnIndexBasedir: 

### + Set variables:

wvecsIndexBasedir="/home/dwaipayan/wvecsIndex/"

wvecsBasedir="/home/dwaipayan/wvecs/"

coll="foo2"

queryTermList="/home/dwaipayan/Dropbox/ir/corpora-stats/topics_xml/301-850-analyzed-query-terms.txt"

k="30"

tempNNPath="/tmp/"$coll"."$k"nn"

nnBasedir="/home/dwaipayan/nn/"

nnIndexBasedir="/home/dwaipayan/nnIndex/"

input=$wvecsBasedir$coll"/"$coll".bin"

output=$nnBasedir$coll"."$k"nn"

### - Set variables:

mkdir -p $nnBasedir
echo "Input file: " $input
echo "Output file: " $output

echo "Starting NN computation..."
../../word2vec/distance_query_words -i $input -t $queryTermList -k $k -o $output
echo "NN computation ended."

cd ..

# Storing them in a Lucene index:
prop_name="nn-index-init.properties"

cat > $prop_name <<EOF
coll=$coll

k=$k

nnBasedir=$nnBasedir

nnIndexBasedir=$nnIndexBasedir

EOF

# ant

java -cp dist/wvecs4j.jar wvec.NNIndexer $prop_name

