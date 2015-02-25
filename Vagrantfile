# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "precise64"

  #

  # Create a public network, which generally matched to bridged network.
  # Bridged networks make the machine appear as another physical device on
  # your network.
  # config.vm.network "public_network"
  def define_node(config, name, ip)
    config.vm.define name do |onyx|
      onyx.vm.box = "precise64"
      onyx.vm.network "private_network", ip: ip
    end
  end

  define_node config, "node1", "10.199.0.101"
  define_node config, "node2", "10.199.0.102"
  define_node config, "node3", "10.199.0.103"

  # Only needed on my stupid laptop
  config.vm.provider "virtualbox" do |vb|
    # Don't boot with headless mode
    vb.gui = true
  end

  #
  # config.vm.provision "chef_client" do |chef|
  #   chef.chef_server_url = "https://api.opscode.com/organizations/ORGNAME"
  #   chef.validation_key_path = "ORGNAME-validator.pem"
  # end
  config.vm.provision "shell", path: "scripts/bootstrap.sh"
end
