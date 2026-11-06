import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * JySphereLibrary is a Robot Framework Library to interact with VMware vSphere API
 *
 * @author mattiap
 */

public class JySphereLibrary {
    public static final String ROBOT_LIBRARY_SCOPE = "TEST SUITE";
    private ServiceInstance si;
    private ServerConnection conn;
    private Folder rootFolder;
    private ManagedEntity[] mes;
    private String user;
    private String passwd;
    private String hosturl;
    public JySphereLibrary() {
        si = null;
    }
    /**
     * Opens a connection to vCenter
     * Example:
     * | Open JySphere Connection | 10.202.4.54 | root | myPassword |
     *
     * @param hostip
     * @param user
     * @param passwd
     * @return
     * @throws Exception
     */
    public String Open_JySphere_Connection(String hostip, String user, String passwd) throws Exception {
        String res = null;
        try {
            this.user = user;
            this.passwd = passwd;
            this.hosturl = "https://" + hostip + "/sdk";
            si = new ServiceInstance(new URL(hosturl), user, passwd, true);
            conn = si.getServerConnection();
            rootFolder = si.getRootFolder();
            mes = rootFolder.getChildEntity();
            res = "1";
        } catch (Exception e) {
            throw new Exception("Cannot login to "+hostip+" : "+e.getMessage());
        }
        return res;
    }
    /**
     * Closes the current connection
     * @throws Exception
     */
    public void Close_JySphere_Connection() throws Exception {
        if (si != null) {
            conn.logout();
        }
    }
    /**
     * Verifies that a VM is powered on
     *
     * @param vmName
     * @throws Exception
     */
    public void VM_Is_Powered_On(String vmName) throws Exception {
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        VirtualMachineRuntimeInfo vmri = (VirtualMachineRuntimeInfo) vm.getRuntime();
        if (vmri.getPowerState() != VirtualMachinePowerState.poweredOn)
            throw new Exception("Virtual Machine '"+vmName+"' is not powered on.");
    }
    /**
     * Verifies that a VM is powered off
     *
     * @param vmName
     * @throws Exception
     */
    public void VM_Is_Powered_Off(String vmName) throws Exception {
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        VirtualMachineRuntimeInfo vmri = (VirtualMachineRuntimeInfo) vm.getRuntime();
        if (vmri.getPowerState() != VirtualMachinePowerState.poweredOff)
            throw new Exception("Virtual Machine '"+vmName+"' is not powered off.");
    }
    /**
     * Verifies that a vSwitch exists within a defined hosts, and that is connected to an vmnic (uplink)
     * `vmnic` can be set to `none` if no uplink exists
     * Examples:
     * | vSwitch Exists | 10.202.4.100 | vSwitch0 | vmnic0 |
     * | vSwitch Exists | 10.202.4.100 | vSwitch2 | none |
     *
     * @param hostName
     * @param vsName
     * @param vmnic
     * @throws Exception
     */
    //test it!
    public void vSwitch_Exists(String hostName, String vsName, String vmnic) throws Exception {
        checkConnection();
        HostSystem host = getHost(hostName);
        HostVirtualSwitch vswitch = getHostVSwitch(host, vsName);
        String[] pnics = vswitch.getPnic();
        if ((vmnic.toLowerCase().equals("none")==false) && (pnics==null))
            throw new Exception("vSwitch is not connected to any nics. (Expected: connected to nic '"+vmnic+"')");
        if (vmnic.toLowerCase().equals("none")){
            if (pnics != null)
                throw new Exception("vSwitch '"+vsName+"' in host '"+hostName+"' is connected to one or more nics.");
            return;
        }
        List <String> pnicsList = new ArrayList();
        for (String pn: pnics) {
            pnicsList.add(pn.split(".PhysicalNic-")[1]);
        }
        if (pnicsList.contains(vmnic)==false){
            throw new Exception("vSwitch '"+vsName+"' in host '"+hostName+"' is not connected to nic '"+vmnic+"'");
        }
    }
    /**
     * Verifies that a PortGroup exists within a host and has the defined vlanId
     * `vlanId` can be set to: 0 (no vlanId), 4095 (All)
     * Examples:
     * | PortGroup Exists | 10.202.4.100 | PG_1    | 555  |
     * | PortGroup Exists | 10.202.4.100 | PG_2     | 0    |
     * | PortGroup Exists | 10.202.4.100 | PG_3 | 4095 |
     *
     * @param hostName
     * @param pgName
     * @param vlanId
     * @throws Exception
     */
    //tested OK
    public void PortGroup_Exists(String hostName, String pgName, int vlanId) throws Exception {
        checkConnection();
        HostSystem host = getHost(hostName);
        HostPortGroup[] portGroups = host.getConfig().getNetwork().getPortgroup();
        HostPortGroup portgroup = null;
        Boolean result = false;
        for (HostPortGroup pg: portGroups) {
            if (pg.getSpec().getName().equals(pgName)){
                portgroup = pg;
                result = true;
                break;
            }
        }
        if (result==false)
            throw new Exception("PortGroup '"+pgName+"' does not exist in host '"+hostName+"'");
        int vlid = portgroup.getSpec().getVlanId();
        if (vlid != vlanId)
            throw new Exception("PortGroup '"+pgName+"' in host '"+hostName+"' has vlanId '"+vlid+"'. ('"+vlanId+"' was expected)");
    }
    /**
     * Verifies that a vSwitch has one or more PortGroups associated to it
     * Example:
     * | vSwitch Contains PortGroups | 10.202.4.100 | vSwitch_1 | PG_1   |
     * | vSwitch Contains PortGroups | 10.202.4.100 | vSwitch_2 | PG_3 | PG_4 |
     *
     * @param hostName
     * @param vsName
     * @param pgNames
     * @throws Exception
     */
    //tested OK
    public void vSwitch_Contains_PortGroups(String hostName, String vsName, String ... pgNames) throws Exception{
        checkConnection();
        HostSystem host = getHost(hostName);
        HostVirtualSwitch vswitch = getHostVSwitch(host, vsName);
        String[] portGroupKeys = vswitch.getPortgroup();
        List <String> portGroupList = new ArrayList();
        for (String pgk: portGroupKeys) {
            portGroupList.add(pgk.split(".PortGroup-")[1]);
        }
        for (String pgn: pgNames){
            if (portGroupList.contains(pgn)==false)
                throw new Exception("vSwitch '"+vsName+"' does not contain PortGroup '"+pgn+"'");
        }
    }
    /**
     * Verifies that a VM is connected to one or more defined PortGroups
     * Example:
     * | VM Is Connected To PortGroups | VM-001 | PG_2  | PG_1    |
     * | VM Is Connected To PortGroups | VM-003 | PG_1 | PG_3 | PG_4 |
     *
     * @param vmName
     * @param pgNames
     * @throws Exception
     */
    //tested OK
    public void VM_Is_Connected_To_PortGroups(String vmName, String ... pgNames) throws
    Exception{
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        Network[] networks = vm.getNetworks();
        List <String> netNamesList = new ArrayList();
        for (Network net: networks){
            netNamesList.add(net.getName());
        }
        for (String pgn: pgNames){
            if (netNamesList.contains(pgn)==false)
                throw new Exception("Virtual Machine '"+vmName+"' is not connected to PortGroup '"+pgn+"'");
        }
    }
    /**
     * Verifies that a host contains all listed VMs
     * Examples:
     * | Host Contains VMs | 10.202.4.100 | VM-001 |
     * | Host Contains VMs | 10.202.4.100 | VM-003 | VM-004 |
     *
     * @param hostName
     * @param vmNames
     * @throws Exception
     */
    //tested OK
    public void Host_Contains_VMs(String hostName, String ... vmNames) throws Exception{
        checkConnection();
        HostSystem host = getHost(hostName);
        VirtualMachine[] vms = host.getVms();
        List <String> vmNamesList = new ArrayList();
        for (VirtualMachine vm: vms){
           vmNamesList.add(vm.getName());
        }
        for (String vmName: vmNames){
            if (vmNamesList.contains(vmName)==false)
                throw new Exception("Virtual Machine '"+vmName+"' is not contained in host '"+hostName+"'");
        }
    }
    /**
     * Verified that the total memory assigned to a VM corresponds to the defined amount,
     expressed in megabytes.
     * Example:
     * | VM Memory Equals | VM-001 | 4096 |
     *
     * @param vmName
     * @param memoryMB
     * @throws Exception
     */
    //tested OK
    public void VM_Memory_Equals(String vmName, String memoryMB) throws Exception{
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        String actualMemory = Integer.toString(vm.getConfig().getHardware().getMemoryMB());
        if (actualMemory.equals(memoryMB)==false)
            throw new Exception("Virtual Machine '"+vmName+"' memory is '"+actualMemory+"' MBs (Expected: '"+memoryMB+"')");
    }
    /**
     * Verifies that a VM has the defined number of network adapters
     * Example:
     * | VM Number Of Network Adapters Equals | VM-002 | 3 |
     *
     * @param vmName
     * @param adapters
     * @throws Exception
     */
    //tested OK
    public void VM_Number_Of_Network_Adapters_Equals(String vmName, int adapters) throws
    Exception{
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        VirtualDevice[] vdevices = vm.getConfig().getHardware().getDevice();
        int actualAdapters = 0;
        for (VirtualDevice vd: vdevices){
            if (vd instanceof VirtualEthernetCard){
                actualAdapters++;
            }
        }
        if (actualAdapters != adapters)
            throw new Exception("Virtual Machine '"+vmName+"' has '"+actualAdapters+"' Network Adapters (Expected: '"+adapters+"')");
    }
    /**
     * Verify that a VM has the specified number of CPUs (cores)
     * Example:
     * | VM CPUs Equals | VM-003 | 7 |
     *
     * @param vmName
     * @param cpus
     * @throws Exception
     */
    //tested OK
    public void VM_CPUs_Equals(String vmName, int cpus) throws Exception{
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        int numCpus = vm.getConfig().getHardware().getNumCPU();
        if (cpus!=numCpus)
            throw new Exception("Virtual Machine '"+vmName+"' has '"+numCpus+"' CPUs (Expected: '"+cpus+"')");
    }
    /**
     * Verifies that Promiscuous Mode is enabled in the defined vSwitch
     *
     * @param hostName
     * @param vsName
     * @throws Exception
     */
    //tested OK
    public void vSwitches_Promiscuous_Mode_Is_Enabled(String hostName, String ... vsName) throws Exception{
        checkConnection();
        HostSystem host = getHost(hostName);
        for (String vs: vsName){
            HostVirtualSwitch vswitch = getHostVSwitch(host, vs);
            if (vswitch.getSpec().getPolicy().getSecurity().getAllowPromiscuous()==false)
                throw new Exception("Promiscuous Mode is disabled for vSwitch '"+vs+"' on host '"+hostName+"'");
        }
    }
    /**
     * Enables the Promiscuous Mode on a vSwitch
     * Example:
     * | Enable vSwitch Promiscuous Mode | 10.202.4.100 | PG_3 |
    */
   //tested OK
    public void Enable_vSwitch_Promiscuous_Mode(String hostName, String vsName) throws Exception{
        checkConnection();
        HostSystem host = getHost(hostName);
        HostVirtualSwitch vswitch = getHostVSwitch(host, vsName);
        vswitch.getSpec().getPolicy().getSecurity().setAllowPromiscuous(true);
        HostNetworkSystem hostNetSystem = host.getHostNetworkSystem();
        hostNetSystem.updateVirtualSwitch(vsName, vswitch.getSpec());
    }
    /**
     * Verify that a VM has a network adapter of a defined type associated with a PortGroup
     * `netAdapterType` can be set to: E1000, VMXNET2, VMXNET3, PCNET32
     * Example:
     * | VM Has Network Adapter | VM-003 | 1 | PG_1    | E1000   |
     * | VM Has Network Adapter | VM-003 | 3 | PG_4 | VMXNET3 |
     *
     *
     * @param vmName
     * @param netAdapter
     * @param pgName
     * @param netAdapterType
     * @throws Exception
     */
   //test it!
    public void VM_Has_Network_Adapter(String vmName, int netAdapter, String pgName, String netAdapterType) throws Exception {
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        VirtualEthernetCard ethCard = getNetworkAdapter(vm, netAdapter);
        String actualPgName = ethCard.getDeviceInfo().getSummary();
        if (actualPgName.equals(pgName) == false)
            throw new Exception("The network adapter 'Network adapter "+netAdapter+"' in Virtual Machine '"+vmName+"' is associated to PortGroup '"+pgName+"' (Expected: '"+pgName+"')");
        String actualAdapterType = "unknown";
        if (ethCard instanceof VirtualE1000)
            actualAdapterType = "E1000";
        else if (ethCard instanceof VirtualVmxnet3)
            actualAdapterType = "VMXNET3";
        else if (ethCard instanceof VirtualVmxnet2)
            actualAdapterType = "VMXNET2";
        else if (ethCard instanceof VirtualPCNet32)
            actualAdapterType = "PCNET32";
        if (actualAdapterType.equals(netAdapterType)==false)
            throw new Exception("The network adapter '"+netAdapter+"' in Virtual Machine '"+vmName+"' is of type '"+actualAdapterType+"' (Expected: '"+netAdapterType+"')");
    }
    public void VM_Has_Passthrough_Device(String vmName, String deviceId) throws Exception {
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        VirtualDevice[] vdevices = vm.getConfig().getHardware().getDevice();
        VirtualEthernetCard ethCard = null;
        VirtualPCIPassthrough found = null;
        for (VirtualDevice vd: vdevices){
            if (vd instanceof VirtualPCIPassthrough){
                VirtualPCIPassthroughDeviceBackingInfo backing = (
                VirtualPCIPassthroughDeviceBackingInfo)vd.getBacking();
                if (deviceId.equals(backing.id)){
                    found = (VirtualPCIPassthrough)vd;
                    break;
                }
            }
        }
        if (found == null)
            throw new Exception("Virtual Machine '"+vmName+"' has no PCI devices connected called '"+deviceId+"'");
    }
    /**
     * Get the name of the Host containing a VM
     * Example:
     * | Get VM Host | VM-001 |
     *
     * @param vmName
     * @return
     * @throws Exception
     */
    public String Get_VM_Host(String vmName) throws Exception {
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        HostSystem host = getVmHost(vm);
        return host.getName();
    }
    /**
     * Gets the MAC address of a specific VM network adapter
     * Example (to get the MAC of the second network adapter):
     * | Get VM MAC Address | VM-001 | 2 |
     *
     * @param vmName
     * @param netAdapter
     * @return
     * @throws Exception
     */
    public String Get_VM_MAC_Address(String vmName, int netAdapter) throws Exception {
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        VirtualDevice[] vdevices = vm.getConfig().getHardware().getDevice();
        VirtualEthernetCard ethCard = null;
        for (VirtualDevice vd: vdevices){
            if (vd instanceof VirtualEthernetCard){
                if (((VirtualEthernetCard)vd).getDeviceInfo().getLabel().equals("Network adapter "+netAdapter)) {
                    ethCard = (VirtualEthernetCard)vd;
                    break;
                }
            }
        }
        if (ethCard == null)
            throw new Exception("Virtual Machine '"+vmName+"' has no network adapters called 'Network adapter "+netAdapter+"'");
        return ethCard.getMacAddress();
    }
    /**
     * Powers on a VM
     *
     * @param vmName
     * @throws Exception
     */
    public void Power_On_VM(String vmName) throws Exception{
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        Task task = vm.powerOnVM_Task(null);
        if (task.waitForTask().equals(Task.SUCCESS))
            System.out.println("Virtual Machine '"+vmName+"' has been powered on");
    }
    /**
     * Powers off a VM
     *
     * @param vmName
     * @throws Exception
     */
    public void Power_Off_VM(String vmName) throws Exception{
        checkConnection();
        VirtualMachine vm = getVm(vmName);
        Task task = vm.powerOffVM_Task();
        if (task.waitForTask().equals(Task.SUCCESS))
            System.out.println("Virtual Machine '"+vmName+"' has been powered off");
    }
//    public void Disconnect_Host(String hostName) throws Exception{
//        checkConnection();
//        HostSystem host = getHost(hostName);
//        Task task = host.disconnectHost();
//        if (task.waitForTask().equals(Task.SUCCESS))
//            System.out.println("Host '"+hostName+"' has been disconnected");
//    }
//
    /**
     * Reconnects a disconnected host to vCenter
     * Example:
     * | Reconnect Host | 10.202.4.199 |
     *
     * @param hostName
     * @throws Exception
     */
    public void Reconnect_Host(String hostName) throws Exception{
        checkConnection();
        HostSystem host = getHost(hostName);
        HostConnectSpec spec = new HostConnectSpec();
        spec.setHostName(hostName);
        spec.setUserName(this.user);
        spec.setPassword(this.passwd);
        try
        {
            Task task = host.reconnectHost_Task(spec);
            String status = task.waitForTask();
            if (status.equals(Task.SUCCESS))
                System.out.println("Host '"+hostName+"' has been reconnected");
            else if(status.equals("error"))
            {
                SSLVerifyFault sslEx = (SSLVerifyFault)task.getTaskInfo().getError().getFault();
                spec.setSslThumbprint(sslEx.getThumbprint());
                task = host.reconnectHost_Task(spec);
                status = task.waitForTask();
                if (status.equals(Task.SUCCESS))
                    System.out.println("Host '"+hostName+"' has been reconnected");
                else
                    throw new Exception("Failed to reconnect host '"+hostName+"'.Error: "+task.getTaskInfo().getError().toString());
            }
        }
        catch(Exception ex)
        {
            System.out.println("Failed to reconnect host '"+hostName+"' Error: '"+ex.getMessage());
        }
    }
    /**
     * Remove a host from vCenter
     * Example:
     * | Remove Host | 10.202.4.199 |
     *
     * @param hostName
     * @throws Exception
     */
    public void Remove_Host(String hostName) throws Exception{
        checkConnection();
        HostSystem host = getHost(hostName);
        if (host.getParent() instanceof ClusterComputeResource)
            throw new Exception("Cannot delete host '"+hostName+"' as it is part of a cluster");
        Task task = host.getParent().destroy_Task();
        if (task.waitForTask().equals(Task.SUCCESS))
            System.out.println("Host '"+hostName+"' has been removed");
        else
            throw new Exception("Errors while attempting to remove a host");
    }
    /**
     * Adds a new host to vCenter
     * Example:
     * | Add Host | 10.202.4.199 | SelfInstall |
     *
     * @param hostName
     * @param datacenterName
     * @throws Exception
     */
    public void Add_Host(String hostName, String datacenterName) throws Exception{
        checkConnection();
        HostConnectSpec spec = new HostConnectSpec();
        spec.setHostName(hostName);
        spec.setUserName(this.user);
        spec.setPassword(this.passwd);
        String license = "M00TK-883D6-RZXC8-Y99H4-90X53";
        Datacenter dc = getDatacenter(datacenterName);
        Folder folder = (Folder) dc.getHostFolder();
        Task task = folder.addStandaloneHost_Task(spec, null, false, license);
        if (task.waitForTask().equals(Task.SUCCESS))
            System.out.println("Host '"+hostName+"' has been added to datacenter '"+dc.getName()+"'");
        else
            throw new Exception("Cannot add host '"+hostName+"' to datacenter '"+dc.getName()+"'");
    }
    /**
     * Verifies that the host has HyperThreading disabled.
     * Note: this check is performed in ESXi, not in the system BIOS.
     *
     * @param hostName
     * @throws Exception
     */
    public void Host_Has_HyperThreading_Disabled(String hostName) throws Exception{
        checkConnection();
        HostSystem host = getHost(hostName);
        if (host.getConfig().getHyperThread().isActive())
            throw new Exception("Host '"+hostName+"' has HyperThreading Enabled (Expected: disabled)");
    }
    public void Host_Has_Passthrough_Enabled_Devices(String hostName, String ... deviceId)
    throws Exception{
        checkConnection();
        HostSystem host = getHost(hostName);
        HostPciPassthruInfo[] ppi = host.getHostPciPassthruSystem().getPciPassthruInfo();
        for (String dev: deviceId){
            Boolean found = false;
            for (HostPciPassthruInfo i : ppi){
                found=false;
                if (i.isPassthruEnabled()){
                    if (dev.equals(i.id)) {
                        found=true;
                        break;
                    }
                }
            }
            if (found==false)
                    System.out.println("Host '"+hostName+"' doesnt have a passthrough enabled device called '"+dev+"'");
        }
    }
    /**
     *  Stops and removes the selected VMs from vCenter
     *  (Note: this keywords ignores
     *
     * @param vmNames
     * @throws Exception
     */
    public void Remove_VMs(String ... vmNames) throws Exception{
        checkConnection();
        VirtualMachine vm = null;
        for (String vmName: vmNames){
            try{
                vm = getVm(vmName);
            }
            catch (Exception e){
                System.out.println("Cannot find VM '"+vmName+"', skipping it");
                continue;
            }
            VirtualMachineRuntimeInfo vmri = (VirtualMachineRuntimeInfo) vm.getRuntime();
            if (vmri.getPowerState() != VirtualMachinePowerState.poweredOff){
                Task task = vm.powerOffVM_Task();
                if (task.waitForTask().equals(Task.SUCCESS))
                    System.out.println("Virtual Machine '"+vmName+"' has been powered off");
                else throw new Exception("Errors powering off Virtual Machine '"+vmName+"'");
            }
            Task task2 = vm.destroy_Task();
            if (task2.waitForTask().equals(Task.SUCCESS))
                System.out.println("Virtual Machine '"+vmName+"' has been removed from vCenter");
            else throw new Exception("Errors attempting to remove from vCenter Virtual Machine '"+vmName+"'");
        }
    }
    /**
     * Remove one or more vSwitches from a host
     * Example:
     * | Remove vSwitches | 10.202.4.199 | vSwitch_1 | vSwitch_2 |
     *
     * @param hostName
     * @param vSwitchesNames
     * @throws Exception
     */
    public void Remove_vSwitches(String hostName, String ... vSwitchesNames) throws Exception {
        checkConnection();
        HostSystem host = getHost(hostName);
        for (String vSwitchName: vSwitchesNames){
            try{
                host.getHostNetworkSystem().removeVirtualSwitch(vSwitchName);
            }
            catch (NotFound e){
                System.out.println("Host '"+hostName+"' does not contain any vSwitch called '"+vSwitchName+"'");
            }
            catch (Exception e){
                System.out.println("vSwitch '"+vSwitchName+"' was not removed. A vSwitch cannot be removed if there are still virtual network adapters connected to it");
            }
        }
    }
    /**
     * Remove one or more PortGroups from a host
     * Example:
     * | Remove PortGroups | 10.202.4.199 | PG_2 |
     *
     * @param hostName
     * @param portGroupsNames
     * @throws Exception
     */
    public void Remove_PortGroups(String hostName, String ... portGroupsNames) throws Exception {
        checkConnection();
        HostSystem host = getHost(hostName);
        for (String pgName: portGroupsNames){
            try{
                host.getHostNetworkSystem().removePortGroup(pgName);
            }
            catch (NotFound e){
                System.out.println("Host '"+hostName+"' does not contain any PortGroup called '"+pgName+"'");
            }
            catch (Exception e){
                System.out.println("PortGroup '"+pgName+"' was not removed. A PortGroup cannot be removed if there are still virtual network adapters connected to it");
            }
        }
    }
    public void Import_OVF(String host_ip, String Ovf_path, String new_vm_name) throws Exception{
        checkConnection();
        //Copy and adapt the code found in class com.vmware.vim25.mo.samples.ovf.ImportLocalOvfToApp
        //from the vijava GitHub repository at https://github.com/benn-cs/vijava
    }
    /* Add keywords:
        - directpath checks
        - what datastore a vm is using
        - verify disk space available (?)
    */
    /*
        PRIVATE METHODS START HERE
    */
    private VirtualMachine getVm(String vmName) throws Exception {
        VirtualMachine vm = (VirtualMachine) new InventoryNavigator(rootFolder).
        searchManagedEntity("VirtualMachine", vmName);
        if (vm==null) throw new Exception("Cannot find any virtual machine called '"+vmName+"'");
        return vm;
    }
    private Datacenter getDatacenter(String dcName) throws Exception {
        Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder).searchManagedEntity("Datacenter", dcName);
        if (dc==null) throw new Exception("Cannot find any Datacenter called '"+dcName+"'");
        return dc;
    }
    private HostSystem getVmHost(VirtualMachine vm) throws Exception {
        ManagedObjectReference mor = vm.getSummary().getRuntime().getHost();
        HostSystem host = new HostSystem(conn, mor);
        return host;
    }
    private HostSystem getHost(String hostName) throws Exception {
        HostSystem host = (HostSystem) new InventoryNavigator(rootFolder).searchManagedEntity("HostSystem", hostName);
        if (host==null) throw new Exception("Cannot find any host called '"+hostName+"'");
        return host;
    }
    private HostVirtualSwitch getHostVSwitch(HostSystem host, String vsName) throws Exception{
        HostVirtualSwitch[] vSwitches = host.getConfig().getNetwork().getVswitch();
        HostVirtualSwitch result = null;
        for (HostVirtualSwitch vs: vSwitches) {
            if (vs.getName().equals(vsName)) {
                result = vs;
                break;
            }
        }
        if (result == null) throw new Exception("Host '"+host.getName()+"' does not contain a vSwitch called '"+vsName+"'");
        return result;
    }
    private VirtualEthernetCard getNetworkAdapter(VirtualMachine vm, int netAdapter) throws Exception{
        VirtualDevice[] vdevices = vm.getConfig().getHardware().getDevice();
        VirtualEthernetCard ethCard = null;
        for (VirtualDevice vd: vdevices){
            if (vd instanceof VirtualEthernetCard){
                if (((VirtualEthernetCard)vd).getDeviceInfo().getLabel().equals("Network adapter "+netAdapter)) {
                    ethCard = (VirtualEthernetCard)vd;
                    break;
                }
            }
        }
        if (ethCard == null)
            throw new Exception("Virtual Machine '"+vm.getName()+"' has no network adapters called 'Network adapter "+netAdapter+"'");
        return ethCard;
    }
    private void checkConnection() throws Exception{
        if ((si == null)||(conn == null)) {
            throw new Exception("Need to establish a connection with 'Open JySphere Connection' first.");
        }
    }
    private HostPortGroup getHostPortGroup(HostSystem host, String pgName) throws Exception{
        HostPortGroup[] portgroups = host.getConfig().getNetwork().getPortgroup();
        HostPortGroup result = null;
        for (HostPortGroup pg: portgroups){
            if (pg.getSpec().getName().equals(pgName)){
                result = pg;
                break;
            }
        }
        if (result == null) throw new Exception("Host '"+host.getName()+"' does not contain a PortGroup called '"+pgName+"'");
        return result;
    }
    private List getVmList(HostSystem host) throws Exception {
        List <VirtualMachine> vmList = new ArrayList();
        try {
            ManagedEntity[] mEntArray = new InventoryNavigator(host).searchManagedEntities("VirtualMachine");
            for (ManagedEntity mEnt : mEntArray) {
                VirtualMachine vm = (VirtualMachine) mEnt;
                vmList.add(vm);
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
        return vmList;
    }
    private boolean vmListContains(List<VirtualMachine> vmList, String vmNameRegex) throws
    Exception {
        Boolean result = false;
        for (VirtualMachine vm : vmList) {
            if (result = vm.getName().matches(vmNameRegex)) {
                break;
            }
        }
        return result;
    }
    private ArrayList vmListFilter(ArrayList<VirtualMachine> vmList, String vmNameRegex) throws Exception {
        ArrayList filteredList = (ArrayList) vmList.clone();
        Iterator <VirtualMachine> it = filteredList.iterator();
        while (it.hasNext()) {
            if (!it.next().getName().matches(vmNameRegex)) {
                it.remove();
            }
        }
        return filteredList;
    }
   // copied from vijava samples (com.vmware.vim25.mo.samples.vm.RemoveVmDisk
    static VirtualDeviceConfigSpec createRemoveDiskConfigSpec(
      VirtualMachineConfigInfo vmConfig, String diskName)
          throws Exception
    {
        VirtualDeviceConfigSpec diskSpec =
            new VirtualDeviceConfigSpec();
        VirtualDisk disk = (VirtualDisk) findVirtualDevice(
            vmConfig, diskName);
        if(disk != null)
        {
          diskSpec.setOperation(
              VirtualDeviceConfigSpecOperation.remove);
          // remove the following line can keep the disk file
          diskSpec.setFileOperation(
              VirtualDeviceConfigSpecFileOperation.destroy);
          diskSpec.setDevice(disk);
          return diskSpec;
        }
        else
        {
          throw new Exception("No device found: " + diskName);
        }
    }
  // copied from vijava samples (com.vmware.vim25.mo.samples.vm.RemoveVmDisk
  private static VirtualDevice findVirtualDevice(
      VirtualMachineConfigInfo cfg, String name)
  {
    VirtualDevice [] devices = cfg.getHardware().getDevice();
    for(int i=0;devices!=null && i<devices.length; i++)
    {
      if(devices[i].getDeviceInfo().getLabel().equals(name))
      {
        return devices[i];
      }
    }
    return null;
  }
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        //JySphereLibrary vi = new JySphereLibrary();
        //vi.Open_JySphere_Connection("10.202.4.54", "root", "myPassword");
        //vi.Import_OVF("10.202.4.134","C:/MP-VM-003/MP-VM-003.ovf","hello");
    }
}
