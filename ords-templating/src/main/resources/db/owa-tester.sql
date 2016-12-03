drop table owa_tester$$

create table owa_tester
( file_name    varchar2(250 BYTE)
, content_type varchar2(250 BYTE)
, content_size number
, binary_file  blob
)$$

create or replace package owa_example as

    procedure execute( pi_request_headers   in     multivalued_parameter_type
                     , pi_request_params    in     multivalued_parameter_type
                     , po_model             out    SYS_REFCURSOR
                     , po_template_path     out    varchar2
                     );

    procedure upload( pi_request_headers    in     multivalued_parameter_type
                     , pi_request_params    in     multivalued_parameter_type
                     , pi_binary            in     binary_type
                     );

    procedure download( pi_request_headers   in     multivalued_parameter_type
                      , pi_request_params    in     multivalued_parameter_type
                      , po_binary_download   out    downloadable_type
                      );

end owa_example;$$

create or replace package body owa_example
as

    procedure execute( pi_request_headers   in     multivalued_parameter_type
                     , pi_request_params    in     multivalued_parameter_type
                     , po_model             out    SYS_REFCURSOR
                     , po_template_path     out    varchar2
                     )
    is
    begin

      po_template_path := '/owa-tester.ftl';

      open po_model for
        select sys_context('USERENV','CURRENT_USER')  as current_user
        from dual;
    end execute;

    procedure upload( pi_request_headers    in     multivalued_parameter_type
                     , pi_request_params    in     multivalued_parameter_type
                     , pi_binary            in     binary_type
                     )
    is
    begin
      insert into owa_tester
      (file_name, content_type, content_size, binary_file)
      values
      (pi_binary.m_filename, pi_binary.m_contenttype, pi_binary.m_filesize, pi_binary.m_buffer);

    end upload;

    procedure download( pi_request_headers   in     multivalued_parameter_type
                      , pi_request_params    in     multivalued_parameter_type
                      , po_binary_download   out    downloadable_type
                      )
    is
      t_file binary_type := binary_type(null,null,null,null);
      t_downloadable_type downloadable_type := downloadable_type(t_file, null);

      r_owa_tester   owa_tester%rowtype;
      lv_filename    owa_tester.file_name%type;
      lv_download_as varchar2(10); -- "inline" or "attachment"
    begin
      lv_filename := pi_request_params.get_value('filename');
      lv_download_as := nvl(pi_request_params.get_value('download-as'), 'inline');

      select otr.*
      into r_owa_tester
      from owa_tester otr
      where otr.file_name = lv_filename;

      t_file.m_buffer := r_owa_tester.binary_file;
      t_file.m_filename := r_owa_tester.file_name;
      t_file.m_contenttype := r_owa_tester.content_type;
      t_file.m_filesize := dbms_lob.getlength(t_file.m_buffer);

      t_downloadable_type.m_binary_file := t_file;
      t_downloadable_type.m_content_disposition := lv_download_as;

      po_binary_download := t_downloadable_type;

    end download;

end owa_example;$$