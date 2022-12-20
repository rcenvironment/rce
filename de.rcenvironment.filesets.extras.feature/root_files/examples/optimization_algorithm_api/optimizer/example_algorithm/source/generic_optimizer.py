from RCE_Optimizer_API import configuration as config
from RCE_Optimizer_API import evaluation as eval
from RCE_Optimizer_API import result

x_0 = str(config.get_design_variable_names()[0])

## print out all configuration options

print "Algorithm: " + str(config.get_algorithm())
print "Constraint names: " + str(config.get_constraint_names())
print "Design variable count: " + str(config.get_design_variable_count())
print "Design variable names: " + str(config.get_design_variable_names())
print "Design variable max. values: " + str(config.get_design_variable_max_values())
print "Design variable min. values: " + str(config.get_design_variable_min_values())
print x_0 + " max. value: " + str(config.get_design_variable_max_value(x_0))
print x_0 + " min. value: " + str(config.get_design_variable_min_value(x_0))
print "Constraint max. values: " + str(config.get_constraint_max_values())
print "Constraint min. values: " + str(config.get_constraint_min_values())
if bool(config.get_constraint_names()):
    c = str(config.get_constraint_names()[0])
    print c + " max. value: " + str(config.get_constraint_max_value(c))
    print c + " min. value: " + str(config.get_constraint_min_value(c))
print "Objective names: " + str(config.get_objective_names())
print "Objective weights: " + str(config.get_objective_weights())
print "Optimization targets: " + str(config.get_optimization_targets())
print "Optimization target of f: " + str(config.get_optimization_target("f"))
print "Initial delta (property): " + str(config.get_property("initial_delta"))
print "Property keys: " + str(config.get_property_keys())
print "Properties: " + str(config.get_properties())
print x_0 + " start value: " + str(config.get_start_value(x_0))
print "Start values: " + str(config.get_start_values())
print "Step values: " + str(config.get_step_values())
print x_0 + " is discrete: " + str(config.is_discrete_variable(x_0))

## Do 100 evaluations
for i in range(0,100):
    print "Run " + str(i)
    start_value = config.get_start_value(str(config.get_design_variable_names()[0]))
    e = eval.evaluate(i, [start_value, 2.0, 3.0])
    
    # print out some responses
    print "Objective value: " + str( e.get_objective_value("f"))
    print "Has f a gradient: " + str(e.has_gradient("f"))
    print "Gradient value:  " + str(e.get_objective_gradient("f"))
    print "Failed inputs: " + str(e.get_failed())
    print "Constraint value: " + str(e.get_constraint_value("c"))
    print "Has c a gradient: " + str(e.has_gradient("c"))
    print "Contraint gradient: " + str(e.get_constraint_gradient("c"))
    
## Optimizer done, print out best evaluation
eval.finalize(1)
