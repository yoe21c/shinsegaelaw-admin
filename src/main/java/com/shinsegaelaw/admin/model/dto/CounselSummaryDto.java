package com.shinsegaelaw.admin.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounselSummaryDto {

    @JsonProperty("comprehensive_evaluation")
    private ComprehensiveEvaluation comprehensiveEvaluation;

    @JsonProperty("business_potential")
    private BusinessPotential businessPotential;

    @JsonProperty("friendliness")
    private Friendliness friendliness;

    @JsonProperty("expertise")
    private Expertise expertise;

    @JsonProperty("communication")
    private Communication communication;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComprehensiveEvaluation {
        @JsonProperty("total_score")
        private Integer totalScore;

        @JsonProperty("summary_scores")
        private SummaryScores summaryScores;

        @JsonProperty("executive_summary")
        private String executiveSummary;

        private List<String> strengths;

        private List<String> weaknesses;

        @JsonProperty("action_items")
        private List<String> actionItems;

        @JsonProperty("consultant_rating")
        private String consultantRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryScores {
        @JsonProperty("business_potential")
        private Integer businessPotential;

        @JsonProperty("legal_expertise")
        private Integer legalExpertise;

        @JsonProperty("communication_clarity")
        private Integer communicationClarity;

        @JsonProperty("customer_friendliness")
        private Integer customerFriendliness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessPotential {
        @JsonProperty("business_score")
        private Integer businessScore;

        @JsonProperty("potential_revenue")
        private String potentialRevenue;

        @JsonProperty("conversion_probability")
        private Integer conversionProbability;

        private String recommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Friendliness {
        @JsonProperty("friendliness_score")
        private Integer friendlinessScore;

        @JsonProperty("relationship_potential")
        private String relationshipPotential;

        @JsonProperty("customer_retention_likelihood")
        private Integer customerRetentionLikelihood;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Expertise {
        @JsonProperty("expertise_score")
        private Integer expertiseScore;

        @JsonProperty("expertise_gaps")
        private String expertiseGaps;

        @JsonProperty("training_needs")
        private String trainingNeeds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Communication {
        @JsonProperty("communication_score")
        private Integer communicationScore;

        @JsonProperty("missed_clarifications")
        private String missedClarifications;

        @JsonProperty("communication_improvements")
        private String communicationImprovements;
    }
}