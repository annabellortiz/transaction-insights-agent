package com.txi.db.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionEntityMappingTest {

    @Test
    public void importedRow_usesJsonTypeMapping_forPostgresJsonb() throws NoSuchFieldException {
        Field importedRow = TransactionEntity.class.getDeclaredField("importedRow");

        assertThat(importedRow.getAnnotation(JdbcTypeCode.class)).isNotNull();
        assertThat(importedRow.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.JSON);
    }
}
