#!/bin/bash -e


COGCOMP_URL=http://cogcomp.cs.illinois.edu/software

START=$PWD
DIRNAME=`dirname "$0"`
CURATOR_BASE=`cd "$DIRNAME" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
TMPDIR=$CURATOR_BASE/tmp
DIST=$CURATOR_BASE/dist
ANNOTATORDIR=$CURATOR_BASE/curator-annotators

echo "$0: setting environment variables..." 

source setEnvVars.sh

echo "$0: About to download required libraries and data files..."


BASIC=1
NEW_NER=1
STANFORD=1
SRL=1
WIKIFIER=1
COREF=1
## NOT READY FOR RELEASE
#QUANTIFIER=0


CLEANUP=0

DWNLDCMD="curl -O"
NOCURL=1

if [ $NOCURL -ne 0 ]; then
    DWNLDCMD="wget -N"
fi


mkdir -p $LIBDIR
mkdir -p $TMPDIR
cd $TMPDIR


#########
# BASIC #
#########

  
if [ $BASIC -eq 1 ]; then
    echo "Downloading LBJ component dependencies..."
 
    $DWNLDCMD $COGCOMP_URL/LBJ-2.8.2.jar
    $DWNLDCMD $COGCOMP_URL/LBJLibrary-2.8.2.jar
    $DWNLDCMD $COGCOMP_URL/LBJPOS.jar
    $DWNLDCMD $COGCOMP_URL/LBJChunk.jar
    $DWNLDCMD $COGCOMP_URL/tools/IllinoisLemmatizer.tgz

    echo "Moving LBJ dependencies..."

    mv LBJ-2.8.2.jar $LIBDIR/LBJ-2.8.2.jar
    mv LBJLibrary-2.8.2.jar $LIBDIR/LBJLibrary-2.8.2.jar
    mv LBJPOS.jar $LIBDIR
    mv LBJChunk.jar $LIBDIR

    tar xzf IllinoisLemmatizer.tgz
    mv IllinoisLemmatizer/dist/* $LIBDIR
    mv IllinoisLemmatizer/lib/* $LIBDIR
    rm -rf $ANNOTATORDIR/illinois-lemmatizer/data
    mv IllinoisLemmatizer/data $ANNOTATORDIR/illinois-lemmatizer/
#    mv IllinoisLemmatizer/config/* $ANNOTATORDIR/illinois-lemmatizer/config/
fi



#######
# NER #
#######


NEREXT_DIR=$CURATOR_BASE/curator-annotators/illinois-ner-extended
NEREXT_CONFIG=$NEREXT_DIR/configs
#NEREXT_DATA=$NEREXT_DIR/data
NERSRC_DIR=illinois-ner-2.8.2


if [ $NEW_NER -eq 1 ]; then

    echo "Downloading Illinois NER component dependencies..."

    $DWNLDCMD $COGCOMP_URL/$NERSRC_DIR.tgz

    echo "Unpacking NER dependencies..."

    tar xzf $NERSRC_DIR.tgz

    echo "Moving NER dependencies..."



    mv $NERSRC_DIR/dist/illinois-ner-2.8.2.jar $LIBDIR/
    mv $NERSRC_DIR/lib/* $LIBDIR
    
fi




###################
# stanford parser #
###################

STANFORDDATADIR=$CURATOR_BASE/curator-annotators/stanford-parser/data

if [ $STANFORD -eq 1 ]; then
    echo "Downloading Stanford parser component dependencies..."

    $DWNLDCMD $COGCOMP_URL/stanford_parser_backup.tgz


    echo "Unpacking Stanford parser dependencies..."

    tar xzf stanford_parser_backup.tgz

    echo "Moving Stanford parser dependencies..."

    mv stanford_backup/stanford-parser-2010-08-16.jar $LIBDIR

    mkdir -p $STANFORDDATADIR
    mv stanford_backup/englishPCFG.ser.gz $STANFORDDATADIR

fi

##############
# QUANTIFIER #
##############

# ******* Quantifier NOT READY for inclusion: build errors/missing classes ******* #


QUANTDIST=illinois-quantifier

if [ $QUANTIFIER -eq 1 ]; then
    
    echo "Downloading Illinois Quantifier component dependencies..."

    $DWNLDCMD $COGCOMP_URL/$QUANTDIST.zip


    echo "Unpacking Quantifier dependencies..."

    unzip $QUANTDIST.zip

    echo "Moving Quantifier dependencies..."

    mv $QUANTDIST/dist/* $LIBDIR
    mv $QUANTDIST/lib/* $LIBDIR

fi


#######
# SRL #
#######


VERBCONFIG=$CURATOR_BASE/curator-annotators/illinois-verb-srl/configs
NOMCONFIG=$CURATOR_BASE/curator-annotators/illinois-nom-srl/configs
SRLDIST=illinoisSRL
SRLVERSION=4.1.1

if [ $SRL -eq 1 ]; then

    echo "Downloading Illinois SRL component dependencies..."


    $DWNLDCMD $COGCOMP_URL/$SRLDIST-$SRLVERSION.tgz

    echo "Unpacking SRL dependencies..."

    tar xzf $SRLDIST-$SRLVERSION.tgz

    echo "Moving SRL dependencies..."

    mv $SRLDIST-$SRLVERSION/dist/* $LIBDIR
    mv $SRLDIST-$SRLVERSION/lib/* $LIBDIR
#    mv $SRLDIST-$SRLVERSION/models/* $LIBDIR
fi






############
# wikifier #
############

WIKIDIST=illinois-wikifier-3.1
WIKIBASE=$CURATOR_BASE/curator-annotators/illinois-wikifier
WIKIFIERDATADIR=$CURATOR_BASE/curator-annotators/illinois-wikifier/data/wikifierData


if [ $WIKIFIER -eq 1 ]; then
    
    echo "Downloading Illinois Wikifier component dependencies..."

    $DWNLDCMD $COGCOMP_URL/$WIKIDIST.tgz


    echo "Unpacking Wikifier dependencies..."

    tar xzf $WIKIDIST.tgz

    echo "Moving Wikifier dependencies..."

    mv $WIKIDIST/dist/wikifier-3.1.jar $LIBDIR/illinois-wikifier-3.1.jar

    mv $WIKIDIST/lib/* $LIBDIR


    mkdir -p $WIKIFIERDATADIR
    mv $WIKIDIST/data/* $WIKIFIERDATADIR
    
fi

 

#########
# COREF #
#########

VERSION=1.5.5-SNAPSHOT

COREF_ACE_DIR=$CURATOR_BASE/curator-annotators/illinois-coref-ace
COREF_ACE_CONFIG=$COREF_ACE_DIR/configs
COREF_ACE_SRC_DIR=illinois-coref-ace-$VERSION
COREF_ACE_SRC_DIST=$COREF_ACE_SRC_DIR

if [ $COREF -eq 1 ]; then

    echo "Downloading Illinois Coreference (ACE) component dependencies..."

    $DWNLDCMD $COGCOMP_URL/$COREF_ACE_SRC_DIST.tgz

    echo "Unpacking Coref dependencies..."

    tar xzf $COREF_ACE_SRC_DIST.tgz

    echo "Moving Coref dependencies..."



    mv $COREF_ACE_SRC_DIR/dist/* $LIBDIR
    mv $COREF_ACE_SRC_DIR/lib/* $LIBDIR
    
fi




#########
# MONGO #
#########

echo "Downloading MongoDB Java driver..."

## NOTE CURL fails on this link due to SSL handshake error; looks like
## a server config error by the host (see 
## https://bugs.launchpad.net/ubuntu/+source/openssl/+bug/861137)



$DWNLDCMD $COGCOMP_URL/mongo-2.7.3.jar
mv mongo-2.7.3.jar $LIBDIR




#######
# END #
#######


if [ $CLEANUP -eq 1 ]; then 
    echo "Cleaning up"
    cd $CURATOR_BASE
    rm -rf $TMPDIR
fi

echo "$0: Done."
