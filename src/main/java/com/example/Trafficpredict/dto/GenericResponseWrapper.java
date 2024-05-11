package com.example.Trafficpredict.dto;

import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenericResponseWrapper<T> {
    private List<T> items;
}
