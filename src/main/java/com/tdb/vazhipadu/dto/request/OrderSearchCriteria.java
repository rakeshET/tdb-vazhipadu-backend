package com.tdb.vazhipadu.dto.request;

import com.tdb.vazhipadu.entity.OrderStatus;
import java.time.LocalDate;

public record OrderSearchCriteria(LocalDate offeringDate, OrderStatus status, String phone) {
}
