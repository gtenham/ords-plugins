drop type multivalued_parameter_type$$

drop type multivalued_type$$

drop type namevalues_type$$

create or replace type values_type as table of varchar2(32760)$$

create or replace type namevalues_type as object(
    m_name     varchar2(512)
   ,m_values   values_type
)$$

create or replace type multivalued_type as table of namevalues_type$$

create or replace type multivalued_parameter_type as object(
    m_parameters multivalued_type

   ,member function get_value(p_name  varchar2)       return varchar2
   ,member function get_values(p_name varchar2)       return values_type
   ,member function value_exists_for(p_name varchar2) return pls_integer

   ,member procedure add_value(p_name varchar2, p_value varchar2)
)$$

create or replace type body multivalued_parameter_type is
    member function value_exists_for(p_name varchar2) return pls_integer is
        l_target_parameter varchar2(512) := lower(p_name);
    begin
        if m_parameters.count = 0 then
            return -1;
        else
            for i in m_parameters.first..m_parameters.last loop
                if( m_parameters(i).m_name = l_target_parameter ) then
                    return i;
                end if;
            end loop;
            return -1;
        end if;
    end;

    member function get_value(p_name varchar2) return varchar2 is
        l_target_parameter varchar2(512) := lower(p_name);
        l_index            pls_integer   := value_exists_for(p_name);
    begin
        if l_index != -1 then
            return m_parameters(l_index).m_values(m_parameters(l_index).m_values.first);
        else
            return null;
        end if;
    end;

    member function get_values(p_name varchar2) return values_type is
        l_target_parameter varchar2(512) := lower(p_name);
        l_index            pls_integer   := value_exists_for(p_name);
    begin
        if l_index != -1 then
            return m_parameters(l_index).m_values;
        else
            return values_type(null);
        end if;
    end;

    member procedure add_value(p_name varchar2, p_value varchar2) is
        l_index pls_integer := value_exists_for(p_name);
    begin
        if l_index != -1 then
            m_parameters(l_index).m_values.extend(1);
            m_parameters(l_index).m_values(m_parameters(l_index).m_values.count) := p_value;
        else
            m_parameters.extend(1);
            m_parameters(m_parameters.count) := namevalues_type(p_name, values_type(p_value));
        end if;

    end;

end;$$