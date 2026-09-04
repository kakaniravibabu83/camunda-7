package com.example.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Compact user summary used inside {@link GroupResponse} — full role details are
 *  redundant there since every member shares the group's own role. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
