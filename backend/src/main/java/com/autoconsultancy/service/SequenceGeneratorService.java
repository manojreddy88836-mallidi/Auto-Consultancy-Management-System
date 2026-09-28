package com.autoconsultancy.service;

import com.autoconsultancy.entity.DatabaseSequence;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final MongoOperations mongoOperations;

    public long generateSequence(String seqName) {
        DatabaseSequence counter = mongoOperations.findAndModify(
                Query.query(Criteria.where("_id").is(seqName)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                DatabaseSequence.class
        );

        return !Objects.isNull(counter) ? counter.getSeq() : 1;
    }

    public void setSequenceIfHigher(String seqName, long value) {
        DatabaseSequence counter = mongoOperations.findOne(
                Query.query(Criteria.where("_id").is(seqName)),
                DatabaseSequence.class
        );
        if (counter == null || counter.getSeq() < value) {
            mongoOperations.save(new DatabaseSequence(seqName, value));
        }
    }

    public void resetSequenceToMax(String collectionName) {
        org.bson.Document doc = mongoOperations.getCollection(collectionName)
                .find()
                .sort(new org.bson.Document("_id", -1))
                .first();
        if (doc != null && doc.get("_id") instanceof Number num) {
            setSequenceIfHigher(collectionName, num.longValue());
        }
    }
}
