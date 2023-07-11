#!/bin/bash

# Get the filename of the CSV file
filename="transacciones_surtidor.csv"

# Get the new first line of the CSV file
new_line="uniq_id_trns,id_trns,id_veh,id_usr,id_eq,veh_site_id,pump_site_id,tank_site_id,user_site_id,prod_id,pump_id,tank_id,department_id,qty,err_cd,ts_start,ts_stop,prod_name,veh_eff,init_vol,final_vol,init_temp,final_temp,vol_comp_15_initial,vol_comp_15_final,qty_comp_15,prod_cd,geo_lat,geo_long,comp_id,industry_id,industry_name"

# Create a temporary file
temp_file="temp.csv"

# Write the new first line to the temporary file
echo $new_line > $temp_file

# Copy the contest of the original file to temp file, skipping the first line
sed '1d' $filename >> $temp_file

# Rename the temporary file to the original CSV file
mv $temp_file $filename
