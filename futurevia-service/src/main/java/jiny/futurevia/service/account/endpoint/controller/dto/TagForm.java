package jiny.futurevia.service.account.endpoint.controller.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagForm {
    private String tagTitle;
}