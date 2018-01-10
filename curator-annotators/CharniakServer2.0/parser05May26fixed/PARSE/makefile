
VPATH=$(THRIFT_CPP_INTERFACE_HOME)


CFLAGS += -I $(THRIFT_CPP_INTERFACE_HOME) -I $(THRIFT_CPP_INCLUDE) -I $(BOOST_INC_DIR) 
LINKFLAGS = -L $(THRIFT_LIB_DIR) -lthrift 


.C.o:
	/usr/bin/g++ $(CFLAGS)  -g -c $<
	/usr/bin/g++ $(CFLAGS) -c -O $<

%.o: %.cpp
	/usr/bin/g++ $(CFLAGS)  -g -c $<



WWBCTEST_OBJS = \
	CharniakException.o\
	AnswerTree.o \
	Bchart.o \
	BchartSm.o \
	FBinaryArray.o \
	CntxArray.o \
	ChartBase.o \
	ClassRule.o \
	CombineBests.o \
	ECArgs.o \
	Edge.o \
	EdgeHeap.o \
	Feat.o \
	Feature.o \
	FeatureTree.o \
	Field.o \
	FullHist.o \
	GotIter.o \
	InputTree.o \
	Item.o \
	Params.o \
	ParseStats.o \
	SentRep.o \
	Term.o \
	TimeIt.o \
	UnitRules.o \
	edgeSubFns.o \
	ewDciTokStrm.o \
	extraMain.o \
	fhSubFns.o \
	headFinder.o \
	headFinderCh.o \
	utils.o \
	MeChart.o \
	wwBCTest.o 


wwBCTest: $(WWBCTEST_OBJS)
	g++ -g $(CFLAGS) $(WWBCTEST_OBJS) -o wwBCTest 


PARSE_OBJS = \
	CharniakException.o\
	AnswerTree.o \
	Bchart.o \
	BchartSm.o \
	FBinaryArray.o \
	CntxArray.o \
	ChartBase.o \
	ClassRule.o \
	CombineBests.o \
	ECArgs.o \
	Edge.o \
	EdgeHeap.o \
	Feat.o \
	Feature.o \
	FeatureTree.o \
	Field.o \
	FullHist.o \
	GotIter.o \
	InputTree.o \
	Item.o \
	Params.o \
	ParseStats.o \
	SentRep.o \
	Term.o \
	TimeIt.o \
	UnitRules.o \
	edgeSubFns.o \
	ewDciTokStrm.o \
	extraMain.o \
	fhSubFns.o \
	headFinder.o \
	headFinderCh.o \
	utils.o \
	MeChart.o 


parseIt: $(PARSE_OBJS) parseIt.o
	g++ -g $(CFLAGS) $(PARSE_OBJS) parseIt.o -o parseIt 


RPARSE_OBJS = \
	CharniakException.o\
	AnswerTree.o \
	Bchart.o \
	BchartSm.o \
	FBinaryArray.o \
	CntxArray.o \
	ChartBase.o \
	ClassRule.o \
	CombineBests.o \
	ECArgs.o \
	Edge.o \
	EdgeHeap.o \
	EgsFromTree.o \
	Feat.o \
	Feature.o \
	FeatureTree.o \
	Field.o \
	FullHist.o \
	GotIter.o \
	InputTree.o \
	Item.o \
	Params.o \
	ParseStats.o \
	SentRep.o \
	Term.o \
	TimeIt.o \
	UnitRules.o \
	edgeSubFns.o \
	ewDciTokStrm.o \
	extraMain.o \
	fhSubFns.o \
	headFinder.o \
	headFinderCh.o \
	utils.o \
	MeChart.o \
	rParse.o 


rParse: $(RPARSE_OBJS)
	g++ $(CFLAGS) $(RPARSE_OBJS) -o rParse 


FPT_OBJS = \
	CharniakException.o\
	ECArgs.o \
	utils.o \
	ParseStats.o \
	finPTest.o

finPTest: $(FPT_OBJS)
	g++ $(CFLAGS) $(FPT_OBJS) -o finPTest



THRIFT_SERVER_OBJS = \
	charniakThriftServer.o CharniakParser.o Parser.o BaseService.o base_types.o curator_types.o

charniakThriftServer: $(THRIFT_SERVER_OBJS) $(PARSE_OBJS) 
	g++ -g $(CFLAGS) $(THRIFT_SERVER_OBJS) $(PARSE_OBJS) -o charniakThriftServer $(LINKFLAGS) 



KBEST_THRIFT_SERVER_OBJS = \
	charniakThriftServer_kBest.o CharniakParser.o MultiParser.o BaseService.o base_types.o curator_types.o

charniakThriftServerKbest: $(KBEST_THRIFT_SERVER_OBJS) $(PARSE_OBJS)
	g++ -g $(CFLAGS) $(KBEST_THRIFT_SERVER_OBJS) $(PARSE_OBJS) -o charniakThriftServerKbest $(LINKFLAGS) 


.PHONY: clean

clean: 
	rm *.o parseIt finPTest charniakThriftServer rparse wwBCTest 
