# FakeNewsChallenge
The goal of the Fake News Challenge is to explore how artificial intelligence technologies, particularly machine learning and natural language processing, might be leveraged to combat the fake news problem. Given a headline and the body of an article, the system classifies the body as either - {irrelevant/discusses/agrees/disagrees} w.r.t the headline.
## Preprocessing
- Stemming: Porter stemming
- Stop words elimination: Used default English stop words list
- Tokenize
## Classification
Uses multi-level classifier with 3 classifiers
- Classifier 1 - classifies if the body is related to the headline or not
- Classifier 2 - classifies if the body merely discusses the headline or opinionated it (either agree or disagree)
- Classifier 3 - classifies if the body agrees or disagress to the stand taken in headline
## Features
- Classifier 1
    1. Bag of words (Jaccard's coefficient)
    2. Tf-Idf
    3. Binary co-occurrence
    4. N-grams (Bi- and Tri-)
- Classifier 2
    1. Hedge words
    2. Word spins {supporting(positive)/refuting(negative)/neutral(nil)}
- Classifier 3
    1. Word spins {supporting(positive)/refuting(negative)/neutral(nil)}
    2. Supporting words
    2. Refuting words
## Dependencies
1. Stanford Core NLP
2. Apache Lucene
3. Wordnet extJWNL
4. SVM - Liblinear
5. Apache Commons - CSV Parser
## How to run
Use Maven to compile and run
1. Compile - `mvn compile`
2. Run - `mvn exec:java`
## Result
1. FNC scoring is in range [2565.5, 7585.25]. My score: 5879.25
2. Accuracy: 81.54% on a dataset of 16000 articles.
