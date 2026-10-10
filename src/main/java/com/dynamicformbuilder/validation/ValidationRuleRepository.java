package com.dynamicformbuilder.validation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidationRuleRepository extends JpaRepository<ValidationRule, Long> {

	List<ValidationRule> findByComponentIdInOrderByIdAsc(Collection<Long> componentIds);

	Optional<ValidationRule> findByIdAndComponentId(Long id, Long componentId);

	boolean existsByComponentIdAndRuleType(Long componentId, ValidationRuleType ruleType);
}
