package org.akhq.modules;

import org.akhq.configs.SchemaRegistryType;
import org.akhq.models.*;
import org.akhq.models.decorators.AvroKeySchemaRecord;
import org.akhq.models.decorators.AvroValueSchemaRecord;
import org.akhq.models.decorators.ProtoBufKeySchemaRecord;
import org.akhq.models.decorators.ProtoBufValueSchemaRecord;
import org.akhq.repositories.AvroWireFormatConverter;
import org.akhq.repositories.CustomDeserializerRepository;
import org.akhq.repositories.RecordRepository;
import org.akhq.repositories.SchemaRegistryRepository;
import org.akhq.utils.ProtobufToJsonDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.Deserializer;

import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.regex.Matcher;

@Singleton
public class RecordFactory {

    private final KafkaModule kafkaModule;
    private final CustomDeserializerRepository customDeserializerRepository;
    private final AvroWireFormatConverter avroWireFormatConverter;
    private final SchemaRegistryRepository schemaRegistryRepository;

    public RecordFactory(KafkaModule kafkaModule,
                         CustomDeserializerRepository customDeserializerRepository,
                         AvroWireFormatConverter avroWireFormatConverter,
                         SchemaRegistryRepository schemaRegistryRepository) {
        this.kafkaModule = kafkaModule;
        this.customDeserializerRepository = customDeserializerRepository;
        this.avroWireFormatConverter = avroWireFormatConverter;
        this.schemaRegistryRepository = schemaRegistryRepository;
    }

    public Record newRecord(ConsumerRecord<byte[], byte[]> record, String clusterId) {
        SchemaRegistryType schemaRegistryType = this.schemaRegistryRepository.getSchemaRegistryType(clusterId);
        Integer keySchemaId = schemaRegistryRepository.determineAvroSchemaForPayload(schemaRegistryType, record.key());
        Integer valueSchemaId = schemaRegistryRepository.determineAvroSchemaForPayload(schemaRegistryType, record.value());

        // base record (default: string)
        Record akhqRecord = new Record(record, keySchemaId, valueSchemaId);

        // avro wire format
        Iterator<Header> contentTypeIter = record.headers().headers("contentType").iterator();
        byte[] value = record.value();
        if (contentTypeIter.hasNext() && value.length > 0 && ByteBuffer.wrap(value).get() != schemaRegistryType.getMagicByte()) {
            String headerValue = new String(contentTypeIter.next().value());
            Matcher matcher = AvroWireFormatConverter.AVRO_CONTENT_TYPE_PATTERN.matcher(headerValue);
            if (matcher.matches()) {
                String subject = matcher.group(1);
                int version = Integer.parseInt(matcher.group(2));
                value = prependWireFormatHeader(value, registryClient, subject, version, magicByte);
            }
        }
        return value;


        // TODO: ,
        //                avroWireFormatConverter.convertValueToWireFormat(record, this.kafkaModule.getRegistryClient(clusterId),
        //                        this.schemaRegistryRepository.getSchemaRegistryType(clusterId)

        Deserializer kafkaAvroDeserializer = this.schemaRegistryRepository.getKafkaAvroDeserializer(clusterId);
        ProtobufToJsonDeserializer protobufToJsonDeserializer = customDeserializerRepository.getProtobufToJsonDeserializer(clusterId);

        // key deserializiation
        if(keySchemaId != null) {
            akhqRecord = new AvroKeySchemaRecord(akhqRecord, kafkaAvroDeserializer);
        } else {
            if(protobufToJsonDeserializer != null) {
                var protoBufKey = new ProtoBufKeySchemaRecord(akhqRecord, protobufToJsonDeserializer);
                if(protoBufKey.getKey() != null) {
                    akhqRecord = protoBufKey;
                }
            }
        }

        // value deserializiation
        if(valueSchemaId != null) {
            akhqRecord = new AvroValueSchemaRecord(akhqRecord, kafkaAvroDeserializer);
        } else {
            if (protobufToJsonDeserializer != null) {
                var protoBufValue = new ProtoBufValueSchemaRecord(akhqRecord, protobufToJsonDeserializer);
                if(protoBufValue.getValue() != null) {
                    akhqRecord = protoBufValue;
                }
            }
        }

        return akhqRecord;
    }

    public Record newRecord(ConsumerRecord<byte[], byte[]> record, RecordRepository.BaseOptions options) {
        return this.newRecord(record, options.getClusterId());
    }
}