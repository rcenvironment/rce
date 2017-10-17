from RCE_Optimizer_API import configuration as config
from RCE_Optimizer_API import evaluation as eval
from RCE_Optimizer_API import result

## print out all configuration options

print config.get_algorithm()
print config.get_base_values()
print config.get_constraint_names()
print config.get_design_variable_count()
print config.get_design_variable_names()
print config.get_design_variable_max_values()
print config.get_design_variable_min_values()
print config.get_constraint_max_values()
print config.get_constraint_min_values()
print config.get_objective_names()
print config.get_objective_weights()
print config.get_property("initial_delta")
print config.get_property_keys()
print config.get_properties()
print config.get_start_value("x")
print config.get_start_values()
print config.get_step_values()
print config.is_discrete_variable("x")


## Do 100 evaluations
for i in range(0,100):
    print "Run " + str(i)
    e = eval.evaluate(i, [float(i), 2.0, 3.0])
    
    # print out some responses
    print e.get_objective_value("f")
    print e.has_gradient("f")
    print e.get_objective_gradient("f")
    print e.get_failed()
    print e.get_constraint_value("c")
    print e.get_constraint_gradient("c")
    
## Optimizer done, print out best evaluation
eval.finalize(1)
