#!/bin/bash

### + Set variables:

# Set the following variables:
## 1. coll: The name of the collecion with which the vector index will be saved
## 2. collSpec: COLLECTION-SPEC-PATH OR,
##    collPath: Collection-doc-path
## 3. textDumpPath: Path to save the text dump
## 4. stopFilePath: Path to the stopword list file
## 5. toAnalyze: Whether to analyze the content before dumping for w2v training or not - true/false
## 6. wvecsBasedir:
## 7. wvecsIndexBasedir:

coll="foo2"

collSpec="/home/dwaipayan/Dropbox/programs/wvec-reproducibility/wvecs4j/build/classes/test/sample-corpora-wt10g.spec"

textDumpPath="/tmp/"$coll".dump"

stopFilePath="/home/dwaipayan/smart-stopwords"

toAnalyze="true"

wvecsBasedir="/home/dwaipayan/wvecs/"

wvecsPath=$wvecsBasedir$coll

wvecPath=$wvecsPath"/"$coll".vec"

wvecsIndexBasedir="/home/dwaipayan/wvecsIndex/"

vectorIndexPath=$wvecsIndexBasedir$coll

# word2vec specific settings


### - Set variables

# + word2vec default settings:
vecsize=200
window=10
negative=5
model=1
model_name="CBOW"
minimum=3
# - word2vec default settings

if [ $# -le 0 ]
then
    echo "Usage: " $0 "
[-collSpec COLLECTION-SPEC-PATH] 
[-collPath COLLECTION-DOC-PATH] 
[-textDumpPath PATH-TO-CREATE-TEXT-DUMP]
[-analyze 1-YES / 0-NO]
[-model 1-CBOW / 0-SKIPGRAM]
";
    # exit 1;
fi

#for var in "$@"
echo $#
#for var in {1..$#}; do
for (( i=1; i<=$#; i++)); do
    j=$((i+1))
    case "${!i}" in
        "-collSpec") echo "Collspec : ${!j}"
                     collSpec=${!j}
                     ;;
        
        "-collPath") echo "CollPath : ${!j}"
                     collPath=${!j}
                     ;;

        "-textDumpPath") echo "textDumpPath : ${!j}"
                     textDumpPath=${!j}
                     echo $textDumpPath
                     ;;

        "-analyze") echo "analyze : ${!j}"
                    analyze=${!j}
                    if [ $analyze = "0" ]
                    then
                        toAnalyze="false"
                    else
                        toAnalyze="true"
                    fi
                    ;;

        "-model") echo "model : ${!j}"
                  model=${!j}
                    if [ $model = "0" ]
                    then
                        echo "Setting Skipgram for word2vec training"
                        modelName = "skipgram"
                    else
                        echo "Setting CBOW for word2vec training"
                        modelName = "CBOW"
                    fi
                    ;;

        esac
done

cd ..

# ant -q

if [ $? -eq 0 ]
then
    echo "*** Build successful ***"
    prop_name="init.properties"

    cat > $prop_name <<EOF
coll=$coll

collSpec=$collSpec

textDumpPath=$textDumpPath

stopFilePath=$stopFilePath

toAnalyze=$toAnalyze

wvecsPath=$wvecsPath

wvecsIndexBasedir=$wvecsIndexBasedir

wvecPath=$wvecPath

EOF
    echo "Running dumpContent.."
    java -cp dist/wvecs4j.jar dumpindex.DumpDoc $prop_name Dwaipayan
    echo "Dumping the content completed..."

    echo "Starting word2vec training with the following setting:"
    echo "Output path: $wvecsPath"
    echo "vecsize: $vecsize"
    echo "window: $window"
    echo "negative sampling window: $negative"
    echo "learning model: $modelName $model"
    echo "min-count: $minimum"
    mkdir -p $wvecsPath
    word2vec -train $textDumpPath -output $wvecsPath"/"$coll -size $vecsize -window $window -sample 1e-4 -negative $negative -hs 0 -cbow $model -iter 5 -min-count $minimum
    echo ""
    echo "word2vec training completed..."

    echo "Indexing word vectors."
    java -cp $CLASSPATH:dist/wvecs4j.jar wvec.WordVecsIndexer $prop_name
    echo "Completed indexing word vectors."


else
    echo "*** Build failed ***" 
    exit 2
fi

