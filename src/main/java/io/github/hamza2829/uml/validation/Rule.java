package io.github.hamza2829.uml.validation;

import io.github.hamza2829.uml.model.Model;
import java.util.List;

public interface Rule {
    List<Finding> evaluate(Model model);
}
