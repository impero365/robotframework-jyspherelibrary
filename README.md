# JySphereLibrary

Robot Framework library for the VMware vSphere API. Library scope is `TEST SUITE`.

Call `Open JySphere Connection` before any other keyword. Most keywords fail if that connection is missing.

## Keywords

- [Add Host](#add-host)
- [Close JySphere Connection](#close-jysphere-connection)
- [Enable vSwitch Promiscuous Mode](#enable-vswitch-promiscuous-mode)
- [Get VM Host](#get-vm-host)
- [Get VM MAC Address](#get-vm-mac-address)
- [Host Contains VMs](#host-contains-vms)
- [Host Has HyperThreading Disabled](#host-has-hyperthreading-disabled)
- [Host Has Passthrough Enabled Devices](#host-has-passthrough-enabled-devices)
- [Import OVF](#import-ovf)
- [Open JySphere Connection](#open-jysphere-connection)
- [PortGroup Exists](#portgroup-exists)
- [Power Off VM](#power-off-vm)
- [Power On VM](#power-on-vm)
- [Reconnect Host](#reconnect-host)
- [Remove Host](#remove-host)
- [Remove PortGroups](#remove-portgroups)
- [Remove VMs](#remove-vms)
- [Remove vSwitches](#remove-vswitches)
- [VM CPUs Equals](#vm-cpus-equals)
- [VM Has Network Adapter](#vm-has-network-adapter)
- [VM Has Passthrough Device](#vm-has-passthrough-device)
- [VM Is Connected To PortGroups](#vm-is-connected-to-portgroups)
- [VM Is Powered Off](#vm-is-powered-off)
- [VM Is Powered On](#vm-is-powered-on)
- [VM Memory Equals](#vm-memory-equals)
- [VM Number Of Network Adapters Equals](#vm-number-of-network-adapters-equals)
- [vSwitch Contains PortGroups](#vswitch-contains-portgroups)
- [vSwitch Exists](#vswitch-exists)
- [vSwitches Promiscuous Mode Is Enabled](#vswitches-promiscuous-mode-is-enabled)

### Add Host

Adds a standalone host to a datacenter. Uses the same user and password as the open connection. Fails if the host cannot be added.

```robotframework
| Add Host | 10.202.4.199 | SelfInstall |
```

### Close JySphere Connection

Logs out of the current vCenter connection.

```robotframework
| Close JySphere Connection |
```

### Enable vSwitch Promiscuous Mode

Enables promiscuous mode on one vSwitch.

```robotframework
| Enable vSwitch Promiscuous Mode | 10.202.4.100 | vSwitch0 |
```

### Get VM Host

Returns the name of the host that runs the VM.

```robotframework
| ${host}= | Get VM Host | VM-001 |
```

### Get VM MAC Address

Returns the MAC address of one VM network adapter. The adapter number matches the vSphere label `Network adapter N` (1-based).

```robotframework
| ${mac}= | Get VM MAC Address | VM-001 | 2 |
```

### Host Contains VMs

Verifies that the host contains every listed VM.

```robotframework
| Host Contains VMs | 10.202.4.100 | VM-001 |
| Host Contains VMs | 10.202.4.100 | VM-003 | VM-004 |
```

### Host Has HyperThreading Disabled

Verifies that HyperThreading is inactive on the host. The check reads the ESXi host configuration, not the system BIOS.

```robotframework
| Host Has HyperThreading Disabled | 10.202.4.100 |
```

### Host Has Passthrough Enabled Devices

Verifies that each listed PCI device id has passthrough enabled on the host.

```robotframework
| Host Has Passthrough Enabled Devices | 10.202.4.100 | 0000:03:00.0 |
| Host Has Passthrough Enabled Devices | 10.202.4.100 | 0000:03:00.0 | 0000:04:00.0 |
```

### Import OVF

Imports a local OVF as a new VM on a host. Arguments are the host IP, the OVF path, and the new VM name. The keyword body is not implemented yet.

```robotframework
| Import OVF | 10.202.4.134 | C:/MP-VM-003/MP-VM-003.ovf | hello |
```

### Open JySphere Connection

Opens a connection to vCenter. Returns `1` on success. Later keywords that add or reconnect a host reuse this user and password.

```robotframework
| Open JySphere Connection | 10.202.4.54 | root | myPassword |
```

### PortGroup Exists

Verifies that a port group exists on the host and has the given VLAN id. Use `0` for no VLAN and `4095` for all VLANs.

```robotframework
| PortGroup Exists | 10.202.4.100 | PG_1 | 555  |
| PortGroup Exists | 10.202.4.100 | PG_2 | 0    |
| PortGroup Exists | 10.202.4.100 | PG_3 | 4095 |
```

### Power Off VM

Powers off a VM and waits for the task to finish.

```robotframework
| Power Off VM | VM-001 |
```

### Power On VM

Powers on a VM and waits for the task to finish.

```robotframework
| Power On VM | VM-001 |
```

### Reconnect Host

Reconnects a disconnected host to vCenter. Retries with the SSL thumbprint when vCenter returns an SSL verify fault.

```robotframework
| Reconnect Host | 10.202.4.199 |
```

### Remove Host

Removes a host from vCenter. Fails if the host belongs to a cluster.

```robotframework
| Remove Host | 10.202.4.199 |
```

### Remove PortGroups

Removes one or more port groups from a host. A missing port group is skipped. A port group that still has virtual network adapters connected to it is left in place.

```robotframework
| Remove PortGroups | 10.202.4.199 | PG_2 |
| Remove PortGroups | 10.202.4.199 | PG_2 | PG_3 |
```

### Remove VMs

Powers off each listed VM if it is still running, then deletes it from vCenter. A name that does not match a VM is skipped.

```robotframework
| Remove VMs | VM-001 |
| Remove VMs | VM-001 | VM-002 |
```

### Remove vSwitches

Removes one or more vSwitches from a host. A missing vSwitch is skipped. A vSwitch that still has virtual network adapters connected to it is left in place.

```robotframework
| Remove vSwitches | 10.202.4.199 | vSwitch_1 | vSwitch_2 |
```

### VM CPUs Equals

Verifies that the VM has the given number of virtual CPUs.

```robotframework
| VM CPUs Equals | VM-003 | 7 |
```

### VM Has Network Adapter

Verifies that a numbered network adapter is attached to the given port group and has the given type. `netAdapter` is 1-based and matches `Network adapter N`. Allowed types are `E1000`, `VMXNET2`, `VMXNET3`, and `PCNET32`.

```robotframework
| VM Has Network Adapter | VM-003 | 1 | PG_1 | E1000   |
| VM Has Network Adapter | VM-003 | 3 | PG_4 | VMXNET3 |
```

### VM Has Passthrough Device

Verifies that the VM has a PCI passthrough device with the given device id.

```robotframework
| VM Has Passthrough Device | VM-001 | 0000:03:00.0 |
```

### VM Is Connected To PortGroups

Verifies that the VM is connected to every listed port group.

```robotframework
| VM Is Connected To PortGroups | VM-001 | PG_2 | PG_1 |
| VM Is Connected To PortGroups | VM-003 | PG_1 | PG_3 | PG_4 |
```

### VM Is Powered Off

Verifies that the VM power state is powered off.

```robotframework
| VM Is Powered Off | VM-001 |
```

### VM Is Powered On

Verifies that the VM power state is powered on.

```robotframework
| VM Is Powered On | VM-001 |
```

### VM Memory Equals

Verifies that the VM memory, in megabytes, equals the given value.

```robotframework
| VM Memory Equals | VM-001 | 4096 |
```

### VM Number Of Network Adapters Equals

Verifies that the VM has the given number of virtual Ethernet adapters.

```robotframework
| VM Number Of Network Adapters Equals | VM-002 | 3 |
```

### vSwitch Contains PortGroups

Verifies that a vSwitch contains every listed port group.

```robotframework
| vSwitch Contains PortGroups | 10.202.4.100 | vSwitch_1 | PG_1 |
| vSwitch Contains PortGroups | 10.202.4.100 | vSwitch_2 | PG_3 | PG_4 |
```

### vSwitch Exists

Verifies that a vSwitch exists on the host and is connected to the given uplink. Set the uplink to `none` when the vSwitch has no physical NIC.

```robotframework
| vSwitch Exists | 10.202.4.100 | vSwitch0 | vmnic0 |
| vSwitch Exists | 10.202.4.100 | vSwitch2 | none   |
```

### vSwitches Promiscuous Mode Is Enabled

Verifies that promiscuous mode is enabled on every listed vSwitch.

```robotframework
| vSwitches Promiscuous Mode Is Enabled | 10.202.4.100 | vSwitch0 |
| vSwitches Promiscuous Mode Is Enabled | 10.202.4.100 | vSwitch0 | vSwitch1 |
```
