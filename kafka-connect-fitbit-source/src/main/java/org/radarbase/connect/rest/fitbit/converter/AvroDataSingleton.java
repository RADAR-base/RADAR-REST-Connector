package org.radarbase.connect.rest.fitbit.converter;

import io.confluent.connect.avro.AvroData;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class AvroDataSingleton {
  private static final AvroDataSingleton instance = new AvroDataSingleton();

  public static AvroDataSingleton getInstance() {
    return instance;
  }

  private final AvroData avroData;
  private final Lock lock;

  private AvroDataSingleton() {
    this.avroData = new AvroData(100);
    lock = new ReentrantLock();
  }

  public SchemaAndValue toConnectData(SpecificRecord record) {
    lock.lock();
    try {
      return this.avroData.toConnectData(record.getSchema(), record);
    } finally {
      lock.unlock();
    }
  }

  public Schema toConnectSchema(org.apache.avro.Schema schema) {
    lock.lock();
    try {
      return this.avroData.toConnectSchema(schema);
    } finally {
      lock.unlock();
    }
  }
}
