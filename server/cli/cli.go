package cli

import (
	"log"
	"maps"
	"os"
	"slices"
	"strings"

	"github.com/chzyer/readline"
)

var commands = map[string]func([]string){
	"new-user":       newUser,
	"user-privilege": userPrivilege,
}
var completions = slices.Collect(maps.Keys(commands))

type completer struct{}

func (completer) Do(line []rune, pos int) (newLine [][]rune, length int) {
	if pos > len(line) {
		pos = len(line)
	}

	// 检查是否已经有空格，如果有则不补全
	lineStr := string(line[:pos])
	if strings.Contains(lineStr, " ") {
		return nil, 0
	}

	// 去除前导空格
	trimmed := strings.TrimLeft(lineStr, " ")
	prefix := trimmed

	// 查找匹配的补全项
	var matches []string
	for _, cmd := range completions {
		if strings.HasPrefix(cmd, prefix) {
			matches = append(matches, cmd)
		}
	}

	if len(matches) == 0 {
		return nil, 0
	}

	// 计算需要替换的长度（考虑前导空格）
	//leadingSpaces := len(lineStr) - len(trimmed)
	replaceLength := len(prefix)

	// 只返回补全部分
	if len(matches) == 1 {
		completion := matches[0][len(prefix):]
		return [][]rune{[]rune(completion)}, replaceLength
	}

	// 多个匹配项
	var result [][]rune
	for _, match := range matches {
		completion := match[len(prefix):]
		result = append(result, []rune(completion))
	}

	return result, replaceLength
}

func StartCLI() {
	rl, err := readline.NewEx(&readline.Config{
		AutoComplete:    completer{},
		HistoryFile:     "readline.history",
		InterruptPrompt: "^C",
		EOFPrompt:       "stop",
	})

	if err != nil {
		log.Printf("Error starting readline, commands will be unavailable: %s", err)
		return
	}
	defer func(rl *readline.Instance) {
		_ = rl.Close()
	}(rl)

	log.Printf("CLI started")
	for {
		line, err := rl.Readline()
		if err != nil {
			break
		}
		if line == "" {
			continue
		}
		if line == "stop" {
			os.Exit(0)
		}

		split := strings.Split(line, " ")
		v, ok := commands[strings.ToLower(split[0])]
		if ok {
			v(split[1:])
		} else {
			log.Printf("Unknown command")
		}
	}
}
